package com.credo.task.client;

import com.credo.task.models.User;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.post;

public class StubLoader {

    private static String mappingsUrl() {return "/__admin/mappings"; }

    public static List<User> readUsersFromFile( Path path){
        try {
            return Json.mapper().readValue(Files.readString(path), new TypeReference<List<User>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read users-all.json", e);
        }
    }

    public static void postMapping(String name, Map<String, String> request, Object responseBody, Integer status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("method", "GET");
        req.put("urlPath", "/users");
        if (!request.isEmpty()) {
            Map<String, Object> qp = new LinkedHashMap<>();
            for (var e : request.entrySet()) {
                qp.put(e.getKey(), Map.of("equalTo", String.valueOf(e.getValue())));
            }
            req.put("queryParameters", qp);
        }
        body.put("request", req);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", status == null ? 200 : status);
        resp.put("headers", Map.of("Content-Type", "application/json"));
        if (responseBody != null) resp.put("jsonBody", responseBody);
        body.put("response", resp);

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .post(mappingsUrl())
                .then()
                .statusCode(201);
    }

    public static void registerPositiveAll(List<User> all){
        postMapping("user-all", Map.of(), all, 200);
    }

    public static void registerFiltered(List<User> all, Map<String, String> qp){
        Predicate<User> predicate = u -> true;
        if(qp.containsKey("age")) {
            int age = Integer.parseInt(qp.get("age"));
            predicate = predicate.and(u -> u.getAge() > age);
        }
        if (qp.containsKey("gender")) {
            String g = qp.get("gender");
            predicate = predicate.and(u -> g.equalsIgnoreCase(u.getGender()));
        }
        List<User> filtered = all.stream().filter(predicate).collect(Collectors.toList());
        postMapping("users-" + qp, qp, filtered, 200);
    }

    public static void register400ForInvalidAge(){
        postMapping("users-age--1", Map.of("age", "-1"), null, 400);
    }

    public static void register500() {
        postMapping("users-500", Map.of("trigger", "500"), null, 500);
    }

    public static void registerGenderUnknownEmpty() {
        postMapping("users-gender-unknown-empty", Map.of("gender", "unknown", "mode", "empty"), List.of(), 200);
    }

    public static void registerGenderUnknown422() {
        postMapping("users-gender-unknown-422", Map.of("gender", "unknown", "mode", "422"), null, 422);
    }
}

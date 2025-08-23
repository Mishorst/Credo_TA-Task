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
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.is;

public class StubLoader {

    public static List<User> readUsersFromFile( Path path){
        try {
            return Json.mapper().readValue(Files.readString(path), new TypeReference<List<User>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read users-all.json", e);
        }
    }

    public static void postMapping(String name, Map<String,String> qp, Object body, int status) {
        Map<String,Object> req = new LinkedHashMap<>();
        req.put("method", "GET");
        req.put("urlPath", "/users");
        if (qp != null && !qp.isEmpty()) {
            Map<String,Object> q = new LinkedHashMap<>();
            qp.forEach((k,v) -> q.put(k, Map.of("equalTo", v)));
            req.put("queryParameters", q);             // IMPORTANT
        }

        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("status", status);
        resp.put("headers", Map.of("Content-Type","application/json"));
        if (body != null) resp.put("jsonBody", body);

        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("request", req);
        payload.put("response", resp);
        payload.put("priority", (qp == null || qp.isEmpty()) ? 10 : 1);  // specific wins

        given().contentType("application/json").body(payload)
                .when().post("http://localhost:8080/__admin/mappings")
                .then().statusCode(anyOf(is(201), is(409)));
    }

    public static void registerFiltered(List<User> all, Map<String,String> qp){
        var filtered = all.stream().filter(u -> matches(u, qp)).toList();
        postMapping("users-" + qp, qp, filtered, 200);
    }

    private static boolean matches(User u, Map<String,String> qp){
        boolean ok = true;
        if (qp.containsKey("age"))    ok &= u.getAge() == Integer.parseInt(qp.get("age"));
        if (qp.containsKey("gender")) ok &= qp.get("gender").equalsIgnoreCase(u.getGender());
        return ok;
    }
}

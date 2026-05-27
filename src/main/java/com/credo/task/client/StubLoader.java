package com.credo.task.client;

import com.credo.task.models.User;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.is;

public class StubLoader {

    public static List<User> readUsersFromFile(Path path) {
        try {
            return Json.mapper().readValue(Files.readString(path), new TypeReference<List<User>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read users file: " + path, e);
        }
    }

    public static void postMapping(String name, Map<String, String> queryParams,
                                   Map<String, String> headers, Object body, int status) {

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", "GET");
        request.put("urlPath", "/users");

        if (queryParams != null && !queryParams.isEmpty()) {
            Map<String, Object> q = new LinkedHashMap<>();
            queryParams.forEach((k, v) -> q.put(k, Map.of("equalTo", v)));
            request.put("queryParameters", q);
        }

        if (headers != null && !headers.isEmpty()) {
            Map<String, Object> h = new LinkedHashMap<>();
            headers.forEach((k, v) -> h.put(k, Map.of("equalTo", v)));
            request.put("headers", h);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("headers", Map.of("Content-Type", "application/json"));
        if (body != null) response.put("jsonBody", body);

        boolean isSpecific = (queryParams != null && !queryParams.isEmpty())
                || (headers != null && !headers.isEmpty());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("request", request);
        payload.put("response", response);
        payload.put("priority", isSpecific ? 1 : 10);

        given().contentType("application/json").body(payload)
                .when().post("http://localhost:8080/__admin/mappings")
                .then().statusCode(anyOf(is(201), is(409)));
    }

    public static void registerFiltered(List<User> all, Map<String, String> queryParams) {
        List<User> filtered = all.stream().filter(u -> matches(u, queryParams)).toList();
        postMapping("users-" + queryParams, queryParams, null, filtered, 200);
    }

    private static boolean matches(User u, Map<String, String> queryParams) {
        boolean ok = true;
        if (queryParams.containsKey("age")) {
            ok &= u.getAge() == parseAgeOrNoMatch(queryParams.get("age"));
        }
        if (queryParams.containsKey("gender")) {
            ok &= queryParams.get("gender").equalsIgnoreCase(u.getGender());
        }
        return ok;
    }

    private static int parseAgeOrNoMatch(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE; // can't equal any real user's age → matches nobody
        }
    }
}
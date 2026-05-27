package com.credo.task.client;

import com.credo.task.models.User;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class UserClient {

    public Response getUsers(Map<String, ?> query) {
        return getUsers(query, null);
    }

    public Response getUsers(Map<String, ?> query, Map<String, ?> headers) {
        RequestSpecification spec = given()
                .queryParams(query == null ? Map.of() : query);

        if (headers != null && !headers.isEmpty()) {
            spec = spec.headers(headers);
        }

        return spec
                .when()
                .get("/users")
                .then()
                .extract()
                .response();
    }

    public List<User> toUsers(Response response) {
        try {
            return Json.mapper().readValue(
                    response.asString(),
                    new TypeReference<List<User>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed for users", e);
        }
    }
}
package com.credo.task.tests;

import com.credo.task.client.UserClient;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UsersApiTest extends UserClient {

    private final UserClient client = new UserClient();

    @DataProvider
    public Object[][] userRequests() {
        return new Object[][]{
            {"testGetAllUsers_Positive", Map.of(), 200, 4, "Alice"},
            {"testFilterByAge_25", Map.of("age", "25"), 200, 2, "Bob"},
            {"testFilterByAge_27", Map.of("age", "27"), 200, 1, "Nutsa"},
            {"testFilterByAge_30", Map.of("age", "30"), 200, 1, "Alice"},
            {"testFilterByGender_Male", Map.of("gender", "male"), 200, 2, "Bob"},
            {"testFilterByGender_Female", Map.of("gender", "female"), 200, 2, "Alice"},
            {"testInvalidAge_Negative", Map.of("age", "-1"), 400, null, null},
            {"testInternalServerError_Negative", Map.of("trigger", "500"), 500, null, null},
            {"testInvalidGender_Empty", Map.of("gender", "unknown", "mode", "empty"), 200, 0, null},
            {"testInvalidGender_422", Map.of("gender","unknown","mode","422"), 422, null, null}
        };
    }

    @Test(dataProvider = "userRequests")
    public void usersParametrized(String name, Map<String, String> query, int expectedStatus,
                                  Integer expectedSize, String expectedFirst){
        Response resp = client.getUsers(query);
        assertThat(resp.statusCode()).isEqualTo(expectedStatus);

        if(expectedStatus == 200){
            var users = client.toUsers(resp);
            if(expectedSize != null) assertThat(users).hasSize(expectedSize);
            if(expectedFirst != null && !users.isEmpty()) assertThat(users.get(0).getName())
                    .isEqualTo(expectedFirst);
        }
    }
}

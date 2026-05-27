package com.credo.task.tests;

import com.credo.task.client.UserClient;
import com.credo.task.models.User;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UsersApiTest extends BaseApiTest {

    private final UserClient client = new UserClient();

    @DataProvider
    public Object[][] userRequests() {
        return new Object[][]{
                {"testGetAllUsers_Positive",     Map.of(),                    Map.of(), 200, 3,    "Alice"},
                {"testFilterByAge_Positive",     Map.of("age", "30"),         Map.of(), 200, 1,    "Alice"},
                {"testFilterByAge_25",           Map.of("age", "25"),         Map.of(), 200, 1,    "Bob"},
                {"testFilterByAge_27",           Map.of("age", "27"),         Map.of(), 200, 1,    "Mariam"},
                {"testFilterByGender_Positive",  Map.of("gender", "male"),    Map.of(), 200, 1,    "Bob"},
                {"testFilterByGender_Female",    Map.of("gender", "female"),  Map.of(), 200, 2,    "Alice"},
                {"testInvalidAge_Negative",      Map.of("age", "-1"),         Map.of(), 400, null, null},
                {"testInvalidGender_Negative",   Map.of("gender", "unknown"), Map.of(), 200, 0,    null},
                {"testInternalServerError_Negative", Map.of(),                Map.of("X-Simulate-Error", "500"), 500, null, null}
        };
    }

    @Test(dataProvider = "userRequests")
    public void usersParametrized(String name,
                                  Map<String, String> query,
                                  Map<String, String> headers,
                                  int expectedStatus,
                                  Integer expectedSize,
                                  String expectedFirst) {

        Response resp = client.getUsers(query, headers);

        assertThat(resp.statusCode())
                .as("status for %s", name)
                .isEqualTo(expectedStatus);

        if (expectedStatus == 200) {
            List<User> users = client.toUsers(resp);

            if (expectedSize != null) {
                assertThat(users).as("size for %s", name).hasSize(expectedSize);
            }
            if (expectedFirst != null && !users.isEmpty()) {
                assertThat(users.get(0).getName())
                        .as("first user for %s", name)
                        .isEqualTo(expectedFirst);
            }
        }
    }
}
package com.credo.task.tests;

import com.credo.task.client.StubLoader;
import com.credo.task.db.TestResultDao;
import com.credo.task.models.User;
import io.restassured.RestAssured;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;

public abstract class BaseApiTest {

    protected static final String WIREMOCK_ADMIN = "http://localhost:8080/__admin/mappings";

    protected static TestResultDao dao;
    private final ConcurrentHashMap<String, Boolean> methodFailed = new ConcurrentHashMap<>();

    @BeforeClass(alwaysRun = true)
    public void setupRestAssured() {
        String base = System.getProperty(
                "api.base",
                System.getenv().getOrDefault("API_BASE", "http://localhost:8080")
        );
        RestAssured.baseURI = base;
        enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeSuite(alwaysRun = true)
    public void prepareWireMockMappings() {
        given().post(WIREMOCK_ADMIN + "/reset").then().statusCode(200);

        List<User> all = StubLoader.readUsersFromFile(Path.of("mocks/__files/users-all.json"));

        // Positive: plain GET /users returns the full list
        StubLoader.postMapping("users-all", Map.of(), null, all, 200);

        // Positive filters: computed from the full list
        StubLoader.registerFiltered(all, Map.of("age", "25"));
        StubLoader.registerFiltered(all, Map.of("age", "27"));
        StubLoader.registerFiltered(all, Map.of("age", "30"));
        StubLoader.registerFiltered(all, Map.of("gender", "male"));
        StubLoader.registerFiltered(all, Map.of("gender", "female"));

        // Negative: invalid age -> 400 (real query param, no trick)
        StubLoader.postMapping("users-age-invalid", Map.of("age", "-1"), null, null, 400);

        // Negative: unknown gender -> empty list
        StubLoader.postMapping("users-gender-unknown", Map.of("gender", "unknown"), null, List.of(), 200);

        // Negative: server error -> 500
        StubLoader.postMapping("users-500", Map.of(), Map.of("X-Simulate-Error", "500"), null, 500);

        dao = new TestResultDao("build/test-results.db");
    }

    @AfterMethod(alwaysRun = true)
    public void recordResult(ITestResult result) {
        String method = result.getMethod().getMethodName();
        boolean failedBefore = methodFailed.getOrDefault(method, false);
        boolean failedNow = !result.isSuccess();
        if (failedNow) methodFailed.put(method, true);

        String status = (failedBefore || failedNow) ? "FAILED" : "PASSED";
        dao.upsert(method, status, LocalDateTime.now());
    }
}
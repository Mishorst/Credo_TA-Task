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

import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;

public abstract class BaseApiTest {

    protected static TestResultDao dao;
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> methodFailed = new java.util.concurrent.ConcurrentHashMap<>();

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
        given().post("http://localhost:8080/__admin/mappings/reset").then().statusCode(200);

        var all = StubLoader.readUsersFromFile(Path.of("mocks/__files/users-all.json"));

        StubLoader.postMapping("users-all", Map.of(), all, 200);

        StubLoader.registerFiltered(all, Map.of("age","25"));
        StubLoader.registerFiltered(all, Map.of("age","27"));
        StubLoader.registerFiltered(all, Map.of("age","30"));
        StubLoader.registerFiltered(all, Map.of("gender","male"));
        StubLoader.registerFiltered(all, Map.of("gender","female"));

        StubLoader.postMapping("users-500", Map.of("trigger","500"), null, 500);
        StubLoader.postMapping("users-age--1", Map.of("age","-1"), null, 400);
        StubLoader.postMapping("users-gender-unknown-empty",
                Map.of("gender","unknown","mode","empty"), List.of(), 200);
        StubLoader.postMapping("users-gender-unknown-422",
                Map.of("gender","unknown","mode","422"), null, 422);

        System.out.println(given().get("http://localhost:8080/__admin/mappings").asPrettyString());

        dao = new TestResultDao("build/test-results.db");
    }

    @AfterMethod(alwaysRun = true)
    public void recordResult(ITestResult result) {
        String method = result.getMethod().getMethodName();
        boolean failedBefore = methodFailed.getOrDefault(method, false);
        boolean failedNow = !result.isSuccess();
        if (failedNow) methodFailed.put(method, true);

        String status = (failedBefore || failedNow) ? "FAILED" : "PASSED";
        dao.upsert(method, status, java.time.LocalDateTime.now());


//        Following code can insert statuses for each DataProvider case,
//          which gives us option to have more clear view of which case failed exactly;

//        String status = result.isSuccess() ? "PASSED" : "FAILED";
//        Object[] args = result.getParameters();
//        String testKey = (args != null && args.length > 0 && args[0] instanceof String s && !s.isBlank())
//                ? s
//                : result.getMethod().getMethodName();
//
//        System.out.printf("DB upsert: key=%s, status=%s%n", testKey, status);
//        dao.upsert(testKey, status, LocalDateTime.now());
    }
}

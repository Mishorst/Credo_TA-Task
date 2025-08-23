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

public abstract class BaseApiTest {

    protected static TestResultDao dao;

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
        dao = new TestResultDao("build/test-results.db");

        List<User> all = StubLoader.readUsersFromFile(Path.of("mocks/__files/users-all.json"));

        StubLoader.registerPositiveAll(all);
        StubLoader.register400ForInvalidAge();
        StubLoader.register500();
        StubLoader.registerGenderUnknownEmpty();
        StubLoader.registerGenderUnknown422();

        StubLoader.registerFiltered(all, Map.of("age", "25"));
        StubLoader.registerFiltered(all, Map.of("age", "27"));
        StubLoader.registerFiltered(all, Map.of("age", "30"));
        StubLoader.registerFiltered(all, Map.of("gender", "male"));
        StubLoader.registerFiltered(all, Map.of("gender", "female"));
    }

    @AfterMethod(alwaysRun = true)
    public void recordResult(ITestResult result) {
        String status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE, ITestResult.SKIP -> "FAILED";
            default -> "FAILED";
        };
        dao.upsert(result.getMethod().getMethodName(), status, LocalDateTime.now());
    }
}

# REST Assured API Test Framework

An enterprise-grade REST API test automation framework built with **Java 11**, **Rest Assured 5.x**, **TestNG 7.x**, **AssertJ**, **ExtentReports**, and **Log4j2**. Targets the free, public [JSONPlaceholder](https://jsonplaceholder.typicode.com) API for full CRUD coverage.

---

## Technology Stack

| Component           | Technology                         | Version      |
| ------------------- | ---------------------------------- | ------------ |
| Language            | Java                               | 11+          |
| Build Tool          | Maven                              | 3.x          |
| Test Framework      | TestNG                             | 7.8.0        |
| API Testing Library | Rest Assured                       | 5.3.2        |
| Assertion Library   | AssertJ + Hamcrest                 | 3.24.2 / 2.2 |
| Reporting           | ExtentReports (Spark)              | 5.1.1        |
| Logging             | Log4j2                             | 2.20.0       |
| JSON Serialisation  | Jackson Databind                   | 2.15.2       |
| Schema Validation   | Rest Assured JSON Schema Validator | 5.3.2        |

---

## Project Structure

```
RestAssuredAPIFramework/
│
├── pom.xml
│
└── src/
    ├── main/java/com/apitests/
    │   ├── base/
    │   │   └── BaseTest.java              ← RequestSpec, ResponseSpec, lifecycle hooks
    │   ├── config/
    │   │   └── ConfigReader.java          ← Loads config.properties via classpath
    │   ├── constants/
    │   │   ├── ApiConstants.java          ← Endpoint paths as constants
    │   │   └── StatusCodes.java           ← HTTP status code constants
    │   ├── models/
    │   │   ├── Post.java                  ← POJO for /posts resource
    │   │   └── User.java                  ← POJO for /users resource
    │   └── utils/
    │       ├── RestUtils.java             ← Reusable GET/POST/PUT/DELETE helpers
    │       ├── ReportManager.java         ← ExtentReports initialisation and logging
    │       └── JsonUtils.java             ← Jackson-based JSON serialisation utilities
    │
    └── test/
        ├── java/com/apitests/
        │   ├── get/
        │   │   └── GetPostsTest.java      ← GET positive + negative tests
        │   ├── post/
        │   │   └── CreatePostTest.java    ← POST positive + negative tests
        │   ├── put/
        │   │   └── UpdatePostTest.java    ← PUT positive + negative tests
        │   ├── delete/
        │   │   └── DeletePostTest.java    ← DELETE positive + negative + idempotency
        │   └── endtoend/
        │       └── CrudFlowTest.java      ← Full CRUD chain: POST→GET→PUT→DELETE→GET(404)
        │
        └── resources/
            ├── config.properties          ← Base URIs, timeouts, report path
            ├── log4j2.xml                 ← Console + file logging configuration
            ├── testng.xml                 ← Suite definition with smoke/regression/negative groups
            └── testdata/
                └── posts_data.json        ← DataProvider source for CreatePostTest
```

---

## File Responsibilities

### `src/main/java`

| File                 | Responsibility                                                                                                                                                                                                                                                    |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `BaseTest.java`      | Builds the shared `RequestSpecification` and `ResponseSpecification`. Hosts `@BeforeSuite` (init report, set base URI, enable HTTPS relaxation), `@AfterMethod` (log pass/fail to ExtentReports), and `@AfterSuite` (flush report). All test classes extend this. |
| `ConfigReader.java`  | Reads `config.properties` from the classpath using a static initialiser. Provides `get()`, `getBoolean()`, `getInt()`, and `getLong()` — zero hard-coded values in test classes.                                                                                  |
| `ApiConstants.java`  | Defines all endpoint path templates as `public static final` strings (e.g. `/posts`, `/posts/{id}`).                                                                                                                                                              |
| `StatusCodes.java`   | Defines HTTP status codes as named integer constants (`OK`, `CREATED`, `NOT_FOUND`, etc.).                                                                                                                                                                        |
| `Post.java`          | Jackson-annotated POJO mirroring the JSONPlaceholder `/posts` resource. `@JsonIgnoreProperties(ignoreUnknown = true)` prevents failures on unknown fields.                                                                                                        |
| `User.java`          | Jackson-annotated POJO mirroring the JSONPlaceholder `/users` resource.                                                                                                                                                                                           |
| `RestUtils.java`     | Stateless helper methods for `GET`, `GET by id`, `POST`, `PUT`, and `DELETE` using the shared `requestSpec` from `BaseTest`.                                                                                                                                      |
| `ReportManager.java` | Wraps the `ExtentReports` lifecycle: `initReport()`, `createTest()`, `logPass()`, `logFail()`, `logInfo()`, and `flushReport()`. Uses `ThreadLocal<ExtentTest>` for thread safety.                                                                                |
| `JsonUtils.java`     | Provides `fromJson()`, `toJson()`, `readJsonArrayFromFile()` (classpath), and `toDataProviderArray()` for converting JSON data to TestNG `Object[][]` format.                                                                                                     |

### `src/test/java`

| File                  | Responsibility                                                                                                                                                                                                                                                           |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `GetPostsTest.java`   | Tests `GET /posts` (list) and `GET /posts/{id}`. Covers: 200 for valid IDs via `@DataProvider`, 404 for invalid IDs, 404 for id=0. Validates status + response time + content-type + body fields on every test.                                                          |
| `CreatePostTest.java` | Tests `POST /posts`. Covers: 201 with valid payload from `posts_data.json` via `@DataProvider`, 201 with empty title (boundary), 201 with missing userId (partial payload).                                                                                              |
| `UpdatePostTest.java` | Tests `PUT /posts/{id}`. Covers: 200 with updated title via `@DataProvider` with two titles, 404 for non-existent ID, 200 with empty body (graceful handling).                                                                                                           |
| `DeletePostTest.java` | Tests `DELETE /posts/{id}`. Covers: 200 for valid IDs via `@DataProvider`, 404 for non-existent ID, 404 for id=0, idempotency (repeated DELETE returns 200).                                                                                                             |
| `CrudFlowTest.java`   | End-to-end CRUD chain in strict dependency order: **Step 1** POST (create, capture id) → **Step 2** GET (fetch, verify) → **Step 3** PUT (update title, verify) → **Step 4** DELETE (remove) → **Step 5** GET non-existent (verify 404). `@AfterClass` performs cleanup. |

### `src/test/resources`

| File                       | Responsibility                                                                                                          |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `config.properties`        | Externalises `base.uri`, `reqres.base.uri`, `log.enabled`, `report.path`, `default.timeout.ms`.                         |
| `log4j2.xml`               | Configures Log4j2 with a Console appender and a File appender writing to `test-output/api-tests.log`.                   |
| `testng.xml`               | Defines three `<test>` blocks running the same classes under `smoke`, `regression`, and `negative` groups respectively. |
| `testdata/posts_data.json` | JSON array of three post objects used as `@DataProvider` input in `CreatePostTest`.                                     |

---

## Response Validation Standard

Every test method validates all four of the following — no exceptions:

```
1. Status Code      — .statusCode(StatusCodes.OK)
2. Response Time    — .time(lessThan(3000L)) via ResponseSpecification
3. Content-Type     — .contentType(ContentType.JSON) via ResponseSpecification
4. Body Fields      — Hamcrest matchers + AssertJ SoftAssertions on extracted POJO
```

---

## Running the Tests

### Full suite via TestNG XML

```bash
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testng.xml
```

### Run by group

```bash
# Smoke tests only
mvn clean test -Dgroups=smoke

# Regression tests only
mvn clean test -Dgroups=regression

# Negative tests only
mvn clean test -Dgroups=negative
```

### Run a specific test class

```bash
mvn clean test -Dtest=GetPostsTest
mvn clean test -Dtest=CrudFlowTest
```

---

## Test Output

| Artifact             | Location                        |
| -------------------- | ------------------------------- |
| ExtentReports HTML   | `test-output/ExtentReport.html` |
| Log4j2 log file      | `test-output/api-tests.log`     |
| Surefire XML reports | `target/surefire-reports/`      |

Open `test-output/ExtentReport.html` in any browser after a test run to view the full interactive report with pass/fail status, response times, and grouped test results.

---

## Configuration

All environment values are read exclusively from `src/test/resources/config.properties`:

```properties
base.uri=https://jsonplaceholder.typicode.com
reqres.base.uri=https://reqres.in/api
log.enabled=true
report.path=test-output/ExtentReport.html
default.timeout.ms=3000
```

To point the suite at a different environment, update `base.uri` — no test class changes required.

---

## Design Principles

- **Zero hard-coded values** in test classes — all config via `ConfigReader`
- **Zero `Thread.sleep()`** — response time validated declaratively via `ResponseSpecification`
- **Zero `System.out.println`** — Log4j2 throughout
- **Self-documenting naming** — test methods follow `test_<Method>_<Resource>_<Scenario>_<ExpectedResult>`
- **Dual assertion strategy** — Rest Assured Hamcrest matchers for inline validation + AssertJ `SoftAssertions` for multi-field POJO validation
- **Separation of concerns** — config, constants, models, utils, base, and tests each in their own package

# SKILL: REST Assured API Testing Framework Generator
## Prompt Engineering Skill using RICE POT Framework

---

## PURPOSE OF THIS SKILL FILE

When this file is provided to a GenAI assistant (Claude, GitHub Copilot, etc.), it must:
1. Read and internalize the full RICE POT prompt below
2. Plan the complete framework structure before writing any code
3. Present the plan and confirm before generating
4. Generate each file step by step, one at a time
5. Produce only runnable, enterprise-grade, zero-bad-practice code

---

## ⚙️ RICE POT MASTER PROMPT

---

### R — ROLE

You are a **Senior QA Automation Architect** with **15+ years of experience** in:
- REST API testing using **Rest Assured (Java)**
- Enterprise-grade test framework design
- CI/CD integration with **Maven** and **TestNG**
- API contract testing, data-driven testing, and reporting

You have deep expertise in:
- Designing layered, maintainable test architectures
- Working with **publicly available REST APIs** (no auth barriers)
- Writing zero-defect, production-ready Java code
- Applying SOLID principles to test code

---

### I — INSTRUCTIONS

#### Framework Generation Rules

**[CRITICAL] Technology Stack — Fixed, Non-Negotiable:**
- Language: **Java 11+**
- Build Tool: **Maven**
- Test Framework: **TestNG**
- API Testing Library: **Rest Assured 5.x**
- Assertion Library: **Hamcrest + AssertJ**
- Reporting: **ExtentReports or Allure**
- Logging: **Log4j2**
- Data Handling: **Jackson (for JSON) + Apache POI (for Excel/data-driven)**
- Config Management: **`.properties` file via `java.util.Properties`**

**[CRITICAL] Target APIs — Use ONLY these free, public, no-auth REST APIs:**
- **JSONPlaceholder** (`https://jsonplaceholder.typicode.com`) — CRUD for Posts, Users, Comments
  - GET `/posts`, GET `/posts/{id}`, POST `/posts`, PUT `/posts/{id}`, DELETE `/posts/{id}`
- **ReqRes** (`https://reqres.in/api`) — User management simulation
  - GET `/users`, GET `/users/{id}`, POST `/users`, PUT `/users/{id}`, DELETE `/users/{id}`
- **Open Library** (`https://openlibrary.org`) — Read-only, for GET demonstrations
- **DummyJSON** (`https://dummyjson.com`) — Full CRUD for products, users, carts

**[CRITICAL] HTTP Methods Coverage — ALL four must be covered with positive + negative test cases:**
- `GET` — Retrieve single and list resources; test 200, 404, invalid ID
- `POST` — Create resource; test 201, 400 (missing required fields), schema validation
- `PUT` — Full update; test 200, 404, 400 (invalid payload)
- `DELETE` — Remove resource; test 200/204, 404, idempotency

**[CRITICAL] TestNG Annotations — Mandatory usage:**
- `@BeforeSuite` — Initialize ExtentReports, read config, set base URI
- `@BeforeClass` — Per-class setup (e.g., create a resource to be used by tests)
- `@BeforeMethod` — Request specification setup (logging, content-type)
- `@Test(groups = {...}, priority = N, dataProvider = "...")` — All test methods
- `@DataProvider` — For data-driven tests (use at least one per HTTP method category)
- `@AfterMethod` — Log pass/fail to report
- `@AfterClass` — Cleanup created test data (DELETE what was POSTed)
- `@AfterSuite` — Flush ExtentReports

**[CRITICAL] Framework Layers — All must be implemented:**
```
src/
├── main/java/
│   └── com/apitests/
│       ├── config/          ← ConfigReader.java
│       ├── constants/       ← ApiConstants.java, StatusCodes.java
│       ├── models/          ← POJO classes (Post.java, User.java, etc.)
│       ├── utils/           ← RestUtils.java, JsonUtils.java, ReportManager.java
│       └── base/            ← BaseTest.java (RequestSpec, ResponseSpec)
└── test/
    ├── java/com/apitests/
    │   ├── get/             ← GetPostsTest.java, GetUsersTest.java
    │   ├── post/            ← CreatePostTest.java, CreateUserTest.java
    │   ├── put/             ← UpdatePostTest.java
    │   ├── delete/          ← DeletePostTest.java
    │   └── endtoend/        ← CrudFlowTest.java (full CRUD chain)
    └── resources/
        ├── config.properties
        ├── testdata/        ← posts_data.json, users_data.csv
        └── testng.xml       ← Suite definition with groups
```

**[MANDATORY] BaseTest.java must contain:**
- Static `RequestSpecification` built with `RestAssured.given()`
- Static `ResponseSpecification` built with `RestAssured.expect()`
- Base URI and Base Path read from `config.properties`
- Request/Response logging via `log().all()` gated by a `config.log.enabled` flag
- `RestAssured.useRelaxedHTTPSValidation()` for HTTPS endpoints

**[MANDATORY] Each Test Class must:**
- Extend `BaseTest`
- Use `@DataProvider` for at least one `@Test` method
- Validate: status code, response time (`< 3000ms`), response body schema (JSON Schema or field-level assertions), content-type header
- Assert using **both** Rest Assured's `body()` matchers **and** AssertJ `assertThat()` — never just one

**[MANDATORY] Response Validation Checklist (apply to every test):**
```java
// 1. Status Code
.statusCode(200)
// 2. Response Time
.time(lessThan(3000L))
// 3. Content-Type
.contentType(ContentType.JSON)
// 4. Body Field Assertion
.body("id", notNullValue())
.body("title", not(emptyString()))
// 5. AssertJ soft assertion on extracted values
SoftAssertions soft = new SoftAssertions();
soft.assertThat(post.getId()).isPositive();
soft.assertAll();
```

**[MANDATORY] Exception Handling:**
- All utility methods throw checked exceptions with meaningful messages
- Test methods catch `AssertionError` and log to report before re-throwing
- Never swallow exceptions silently

**[MANDATORY] Naming Conventions:**
- Test methods: `test_<Method>_<Resource>_<Scenario>_<ExpectedResult>` e.g., `test_GET_Post_ValidId_Returns200`
- POJO classes: match API model names exactly
- Constants: `UPPER_SNAKE_CASE` in dedicated constants class

**[OUTPUT] Generate in this exact sequence — one file at a time:**
1. `pom.xml`
2. `config.properties`
3. `ApiConstants.java`
4. `StatusCodes.java`
5. `Post.java` (POJO)
6. `User.java` (POJO)
7. `ConfigReader.java`
8. `RestUtils.java`
9. `ReportManager.java`
10. `JsonUtils.java`
11. `BaseTest.java`
12. `GetPostsTest.java`
13. `CreatePostTest.java`
14. `UpdatePostTest.java`
15. `DeletePostTest.java`
16. `CrudFlowTest.java` (end-to-end CRUD chain)
17. `testng.xml`
18. `posts_data.json` (DataProvider source)

**[DON'T] Never do any of the following:**
- `Thread.sleep()` anywhere — use Rest Assured's `.await()` or TestNG retry
- Hard-coded URLs or credentials in test classes — always from `config.properties`
- Static imports that obscure what library is being used
- Test methods with logic that mixes setup + assertion + cleanup in one block
- Comments explaining what the code does (code must be self-documenting via naming)
- Unused imports
- `System.out.println` — use `Log4j2` logger only
- Empty catch blocks
- Magic numbers/strings — use constants

---

### C — CONTEXT

You are building an **enterprise-grade REST API test automation framework** that will be:
- Used by a QA team of 5–10 engineers on a real project
- Integrated into a **Jenkins/GitHub Actions CI/CD pipeline**
- Run daily in a regression suite against a **staging environment**
- Maintained over 2+ years, requiring high readability and zero technical debt

The framework uses **JSONPlaceholder** and **ReqRes** as the target APIs because they are:
- Publicly accessible with no API key requirement
- Stable, well-documented, and predictable
- Covering full CRUD operations making them ideal for demo frameworks
- Safe for inclusion in a public GitHub repository

The **end-to-end CRUD flow** test (`CrudFlowTest.java`) must simulate a real user journey:
1. **POST** → Create a new post → capture `id` from response
2. **GET** → Fetch the created post by `id` → verify data matches
3. **PUT** → Update the post title → verify updated value
4. **DELETE** → Delete the post → verify 200/204
5. **GET** → Attempt to fetch deleted post → verify 404

---

### E — EXAMPLE

#### Example: BaseTest.java structure
```java
public class BaseTest {

    protected static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;

    @BeforeSuite
    public void globalSetup() {
        RestAssured.useRelaxedHTTPSValidation();
        requestSpec = new RequestSpecBuilder()
            .setBaseUri(ConfigReader.get("base.uri"))
            .setContentType(ContentType.JSON)
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .build();

        responseSpec = new ResponseSpecBuilder()
            .expectResponseTime(lessThan(3000L))
            .expectContentType(ContentType.JSON)
            .build();

        ReportManager.initReport();
    }

    @AfterSuite
    public void globalTeardown() {
        ReportManager.flushReport();
    }
}
```

#### Example: Test Method structure
```java
@Test(dataProvider = "validPostIds", groups = {"smoke", "regression"})
public void test_GET_Post_ValidId_Returns200(int postId) {
    Response response = given(requestSpec)
        .pathParam("id", postId)
        .when()
        .get(ApiConstants.POST_BY_ID)
        .then()
        .spec(responseSpec)
        .statusCode(StatusCodes.OK)
        .body("id", equalTo(postId))
        .body("title", not(emptyString()))
        .extract()
        .response();

    Post post = response.as(Post.class);
    SoftAssertions soft = new SoftAssertions();
    soft.assertThat(post.getId()).isEqualTo(postId);
    soft.assertThat(post.getTitle()).isNotBlank();
    soft.assertThat(post.getUserId()).isPositive();
    soft.assertAll();
}

@DataProvider(name = "validPostIds")
public Object[][] validPostIds() {
    return new Object[][]{{1}, {5}, {10}};
}
```

#### Example: POJO (Post.java)
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {
    private int id;
    private int userId;
    private String title;
    private String body;
    // getters, setters, @JsonProperty if needed
}
```

#### Example: ConfigReader.java
```java
public class ConfigReader {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
```

#### Example: config.properties
```properties
base.uri=https://jsonplaceholder.typicode.com
reqres.base.uri=https://reqres.in/api
log.enabled=true
report.path=test-output/ExtentReport.html
default.timeout.ms=3000
```

---

### P — PARAMETERS

| Parameter | Value |
|---|---|
| Code Quality | Production-level, zero bad practices |
| Accuracy | Pin-point, no approximations |
| Java Version | Java 11+ (use `var` where appropriate) |
| Rest Assured Version | 5.3.x (latest stable) |
| TestNG Version | 7.8.x |
| ExtentReports Version | 5.x |
| Assertion Style | AssertJ `SoftAssertions` + Rest Assured Hamcrest matchers |
| Data-Driven Coverage | Minimum 1 `@DataProvider` per test class |
| Negative Test Coverage | Minimum 2 negative cases per HTTP method |
| Response Validation | Status + Time + ContentType + Body schema — ALL four, every test |
| Logging | Log4j2 only, no `System.out` |
| Config Externalisation | 100% — zero hard-coded values in test classes |

---

### O — OUTPUT

Deliver the following and **nothing else**:

| # | Artifact | Location |
|---|---|---|
| 1 | `pom.xml` | project root |
| 2 | `config.properties` | `src/test/resources/` |
| 3 | `ApiConstants.java` | `src/main/java/com/apitests/constants/` |
| 4 | `StatusCodes.java` | `src/main/java/com/apitests/constants/` |
| 5 | `Post.java` | `src/main/java/com/apitests/models/` |
| 6 | `User.java` | `src/main/java/com/apitests/models/` |
| 7 | `ConfigReader.java` | `src/main/java/com/apitests/config/` |
| 8 | `RestUtils.java` | `src/main/java/com/apitests/utils/` |
| 9 | `ReportManager.java` | `src/main/java/com/apitests/utils/` |
| 10 | `JsonUtils.java` | `src/main/java/com/apitests/utils/` |
| 11 | `BaseTest.java` | `src/main/java/com/apitests/base/` |
| 12 | `GetPostsTest.java` | `src/test/java/com/apitests/get/` |
| 13 | `CreatePostTest.java` | `src/test/java/com/apitests/post/` |
| 14 | `UpdatePostTest.java` | `src/test/java/com/apitests/put/` |
| 15 | `DeletePostTest.java` | `src/test/java/com/apitests/delete/` |
| 16 | `CrudFlowTest.java` | `src/test/java/com/apitests/endtoend/` |
| 17 | `testng.xml` | `src/test/resources/` |
| 18 | `posts_data.json` | `src/test/resources/testdata/` |

**Format rules:**
- Each file starts with a single-line header comment: `// FILE: <relative path>`
- No other comments anywhere
- No explanations between files
- No markdown prose — code blocks only
- Files separated by a single blank line

---

### T — TONE

```
Technical • Precise • Enterprise-grade • Zero-tolerance for bad practice
Self-documenting code • Naming over comments • Consistency over cleverness
```

---

## 🔁 HOW TO USE THIS SKILL FILE

### Step 1 — Provide this file to your GenAI assistant
Paste the entire content of this file into your conversation with Claude, GitHub Copilot Chat, or any LLM.

### Step 2 — Trigger the framework generation with this exact instruction:
```
Read the SKILL.md I have provided. 
First, show me your complete plan: the folder structure, each file you will generate, 
and what each file's responsibility is.
Wait for my confirmation before generating any code.
Then generate each file one by one in the sequence defined in the OUTPUT section.
```

### Step 3 — After plan confirmation, trigger file-by-file generation:
```
Plan confirmed. Begin generating. Start with file 1: pom.xml
```
Then after each file:
```
Next file.
```

### Step 4 — Validate the output
Run the following to confirm the framework compiles and tests execute:
```bash
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testng.xml
```

---

## ✅ QUALITY CHECKLIST (GenAI must self-verify before delivering each file)

Before outputting any file, verify:

- [ ] Zero `Thread.sleep()` calls
- [ ] Zero hard-coded URLs or IDs in test classes
- [ ] Every test validates: status code + response time + content-type + body
- [ ] `@DataProvider` used in at least the GET and POST test classes
- [ ] Both positive AND negative test cases present for each HTTP method
- [ ] `SoftAssertions` used for multi-field body validation
- [ ] All TestNG annotations applied correctly with groups and priorities
- [ ] Log4j2 used for all logging — zero `System.out.println`
- [ ] All POJOs have `@JsonIgnoreProperties(ignoreUnknown = true)`
- [ ] `ConfigReader` loads all environment values
- [ ] `ReportManager` initialised in `@BeforeSuite`, flushed in `@AfterSuite`
- [ ] `CrudFlowTest` chains POST → GET → PUT → DELETE → GET(404)
- [ ] `testng.xml` defines groups: `smoke`, `regression`, `negative`
- [ ] `pom.xml` includes all required dependencies with correct versions

---

## 📦 DEPENDENCY VERSIONS (use these exact versions in pom.xml)

```xml
<!-- Rest Assured -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.3.2</version>
</dependency>

<!-- TestNG -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.8.0</version>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.2</version>
</dependency>

<!-- Jackson Databind -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- ExtentReports -->
<dependency>
    <groupId>com.aventstack</groupId>
    <artifactId>extentreports</artifactId>
    <version>5.1.1</version>
</dependency>

<!-- Log4j2 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.20.0</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.20.0</version>
</dependency>

<!-- JSON Schema Validator (for schema validation tests) -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>5.3.2</version>
</dependency>

<!-- Hamcrest (transitive via Rest Assured, but explicit for clarity) -->
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest</artifactId>
    <version>2.2</version>
</dependency>
```

---

## 🗂️ COMPLETE FOLDER STRUCTURE (reference for the GenAI)

```
rest-assured-api-framework/
│
├── pom.xml
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/apitests/
│   │           ├── base/
│   │           │   └── BaseTest.java
│   │           ├── config/
│   │           │   └── ConfigReader.java
│   │           ├── constants/
│   │           │   ├── ApiConstants.java
│   │           │   └── StatusCodes.java
│   │           ├── models/
│   │           │   ├── Post.java
│   │           │   └── User.java
│   │           └── utils/
│   │               ├── RestUtils.java
│   │               ├── ReportManager.java
│   │               └── JsonUtils.java
│   │
│   └── test/
│       ├── java/
│       │   └── com/apitests/
│       │       ├── get/
│       │       │   └── GetPostsTest.java
│       │       ├── post/
│       │       │   └── CreatePostTest.java
│       │       ├── put/
│       │       │   └── UpdatePostTest.java
│       │       ├── delete/
│       │       │   └── DeletePostTest.java
│       │       └── endtoend/
│       │           └── CrudFlowTest.java
│       │
│       └── resources/
│           ├── config.properties
│           ├── log4j2.xml
│           ├── testng.xml
│           └── testdata/
│               └── posts_data.json
```

---

*SKILL version: 1.0 | Framework: Rest Assured + Java + Maven + TestNG | Target APIs: JSONPlaceholder, ReqRes*

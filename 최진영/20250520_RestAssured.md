## RestAssured란?
`RestAssured`는 RESTful 애플리케이션의 http 앤드포인트의 테스트를 편리하게 하기 위한 Java의 라이브러리입니다. 또한 HTTP를 실제로 날리는 방식의 테스트 도구입니다. 따라서 테스트를 실행할 때, 서버가 실제로 떠있어야 요청을 보낼 수 있습니다.

서버가 실제로 떠있어야 요청을 보낼 수 있다는 이유 때문에 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`와 같이 작성해줄 필요가 있습니다.

SpringBootTest.WebEnvironment의 옵션으로는 `MOCK, RANDOM_PORT, DEFINED_PORT, NONE` 등이 있습니다.
> `MOCK`은 내장 서블릿 컨테이너 없이 디스페처서블릿만 동작하기 때문에 `RestAssured`에서 실제 HTTP 요청이 불가능합니다. 마찬가지로 `NONE` 속성 또한 웹 서버 없이 애플리케이션만 로딩되기 때문에 `RestAssured`를 사용할 수 없게 됩니다.

## RestAssured의 주요 기능

`RestAssured`의 주요 기능은 다음과 같습니다.
- 쉽게 테스트를 작성할 수 있는 API
- XML 및 JSON 파싱 지원
- Java 생태계 도구(JUnit, TestNG 등)와의 통합
- 광범위한 검증 기능
  - HTTP 상태코드 검증
    - `.then().statusCode(200);`
  - 응답 헤더 검증
    - `.then().header("Content-Type", equalTo("application/json"));`
  - 응답 바디(JSON)의 필드 값 검증
    - `.then().body("user.name", equalTo("진영"));`
  - 응답 바디의 구조 및 조건 검증
    - `.then().body("items.size()", greaterThan(0));`
  - JSON Path를 활용한 깊은 구조 검증
    - `.then().body("data[0].attributes.email", containsString("@"));`
  - 컬렉션 형태의 값 검증
    - `.then().body("roles", hasItems("USER", "ADMIN"));`

## RestAssured 주요 활용처
`RestAssured`는 Spring Boot 통합 테스트에서 실제 HTTP 통신을 흉내내거나 직접 수행하는데 특화된 라이브러리입니다. 따라서 인수 테스트 혹은 통합 테스트에서 자주 사용됩니다.
> 💡 인수 테스트가 통합 테스트보다 좀 더 큰 범위인 듯 합니다. '회원가입 후 로그인' 처럼 사용자가 접할 수 있는 특정 시나리오를 기반으로 여러 API를 순차적으로 테스트하는 것이고, 통합 테스트는 둘 이상의 모듈을 통합해서 테스트하는 것을 의미한다고 합니다.
## 기본 RestAssured API 테스트 개념
`RestAssured`는 행동 기반 개발(BDD)에서 영감을 받은 `Given-When-Then` 구문을 따릅니다. 이 구조는 코드베이스에 익숙하지 않은 사람에게도 테스트를 읽기 쉽고 직관적으로 만듭니다.

### MockMVC와의 코드 비교
```java
// RestAssured
@Test
void 회원가입_성공_테스트() {
    SignupRequestDto request = new SignupRequestDto("jinyoung", "1234");

    given()
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(request)
    .when()
        .post("/api/signup")
    .then()
        .statusCode(201)
        .body("message", equalTo("회원가입 성공"));
}

// MockMvc
@Test
void 회원가입_성공_테스트() throws Exception {
    // given
    SignupRequestDto request = new SignupRequestDto("jinyoung", "1234");
    String json = objectMapper.writeValueAsString(request);

    // when & then
    mockMvc.perform(post("/api/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("회원가입 성공"));
}
```

> 다들 MockMvc를 통해 테스트를 하다보면 주석을 통해 given, when, then을 나눠서 작성했을거라 생각합니다. RestAssured를 사용하면 그럴 필요가 없고 더 직관적이라는 생각이 들었습니다.

## (개인적인 의견) RestAssured vs MockMvc
> `MockMvc`도 통합 테스트를 할 수 있는데, 무슨 차이가 있는 거고 각각이 어떤 장단점을 바탕으로 어떤 상황에서 선택되어야할 지 궁금했습니다. 개인적인 의견이니 편하게 들어주세요.

아무래도 `RestAssured` 같은 경우는 `MockMvc`보다는 좀 더 실제에 가까운 테스트에 적합한 도구인 것 같습니다.
반면에 `MockMvc`는 `@WebMvcTest` 어노테이션과 함께 `Controller` 레이어의 단위테스트를 할 때 사용되는 것이 좋다고 판단됩니다.

왜냐하면, `RestAssured`는 `@SpringBootTest`와 같이 사용되어야해서 단위 테스트에 사용하기엔 속도 측면에서 단점이 있다고 생각하기 때문입니다.
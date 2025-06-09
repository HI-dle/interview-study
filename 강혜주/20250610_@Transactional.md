## 1. `@Transactional`의 동작 원리

### 트랜잭션 시작과 커넥션 획득

```java
@Service
public class ProductService {
  @Transactional
  public void saveProduct(Product product) {
    productRepository.save(product);
  }
}
```

- 스프링은 `@Transactional`이 붙은 메서드에 대해 **프록시 객체**를 생성하고, AOP 방식으로 트랜잭션을 관리한다.
    - @Transactional을 메소드 또는 클래스에 명시하면 AOP를 통해 Target이 상속하고 있는 인터페이스 또는 Target 객체를 상속한 Proxy 객체가 생성되며, Proxy 객체의 메소드를 호출하면 Target 메소드 전 후로 트랜잭션 처리를 수행한다.

- **실제 DB 커넥션은 첫 DB 작업이 실행될 때 획득**된다. 이 전까지는 커넥션을 점유하지 않는다.

- 메서드가 정상 종료되면 커밋되고, 예외 발생 시 롤백되며, 커넥션은 풀로 반환된다.


### 프록시 동작 원리와 제약사항

스프링은 **프록시 패턴**을 사용해 트랜잭션을 관리하는데, 이는 다음과 같은 중요한 제약사항을 가진다.
####  접근 제어자 제약

```java
@Service
public class UserService {
    
    @Transactional //  private 메서드는 프록시 적용 불가
    private void createUserInternal(String name) {
        userRepository.save(new User(name));
    }
    
    @Transactional //  protected도 권장되지 않음
    protected void processUser(User user) {
        // 로직...
    }
    
    @Transactional // public 메서드만 정상 동작
    public void createUser(String name) {
        userRepository.save(new User(name));
    }
}
```

**Spring Boot 2.0.0부터 CGLib가 기본값**으로 설정되어 있다.
- `private` 메서드는 상속이 불가능해서 프록시 생성이 막힌다
- `protected` 메서드는 기술적으로 가능하지만 **AOP 목적으로는 권장되지 않는다
    - 공식문서 번역 [Declaring a Pointcut](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html#aop-pointcuts-designators)
        1. **프록시 유형별 접근 제어자 지원**
            - "For JDK proxies, only public interface method calls on the proxy can be intercepted. With CGLIB, public and protected method calls on the proxy are intercepted (and even package-visible methods, if necessary)."
              **JDK 프록시**: `public` 인터페이스 메서드만 **CGLib 프록시**: `public`, `protected`, package-visible 메서드
        2. **권장사항과 주의사항**
           "However, common interactions through proxies should always be designed through public signatures."
           **Spring의 권장사항**: 프록시를 통한 상호작용은 항상 public 시그니처로 설계하라.
           "Note that pointcut definitions are generally matched against any intercepted method. If a pointcut is strictly meant to be public-only, even in a CGLIB proxy scenario with potential non-public interactions through proxies, it needs to be defined accordingly."
           **포인트컷 정의 시 주의**: CGLib에서 non-public 메서드도 인터셉트될 수 있으므로, public-only로 제한하려면 포인트컷을 명시적으로 정의해야 함.
            - 참고
            - https://www.inflearn.com/community/questions/754189/default%EC%99%80-protected-%EC%A0%91%EA%B7%BC-%EC%A0%9C%ED%95%9C%EC%9E%90-transactional-%EC%A0%81%EC%9A%A9?srsltid=AfmBOoo_9HHuOvr1oHDJ2N9PvCtZflH5Ume1-OS2tb1yDCsCRloGF8Kz

### 트랜잭션 전파 방식

| 속성             | 설명                          |
| -------------- | --------------------------- |
| REQUIRED (기본값) | 기존 트랜잭션에 참여하거나 없으면 새로 시작    |
| REQUIRES_NEW   | 항상 새 트랜잭션 시작, 기존 트랜잭션 일시 중단 |
| SUPPORTS       | 트랜잭션이 있으면 참여, 없으면 비트랜잭션     |
| NOT_SUPPORTED  | 트랜잭션 없이 실행, 기존 트랜잭션 일시 중단   |
| MANDATORY      | 반드시 트랜잭션 존재 필요, 없으면 예외      |
| NEVER          | 트랜잭션이 있으면 예외 발생             |
| NESTED         | 중첩 트랜잭션 시작 (DB가 지원할 경우)     |

특히 `REQUIRES_NEW`는 **별도의 커넥션을 할당**하기 때문에 DB 커넥션 관리에 주의가 필요하다.


---

## 2. 트랜잭션과 커넥션 관계

### 커넥션은 언제 얻고 언제 반환될까?

```java
@Transactional 
public void processData() {     
    doSomethingWithoutDB();              // 커넥션 없음     
    User user = userRepository.findById(1L); // 커넥션 획득     
    userRepository.save(user);          // 같은 커넥션 사용 
} // 종료 시 커밋 및 커넥션 반환
```

- 트랜잭션 시작만으로 커넥션을 획득하진 않는다.

- 첫 DB 작업 시점에서 커넥션을 가져오고, 트랜잭션 내에서 동일 커넥션을 재사용한다.

- 메서드 종료 시 트랜잭션 종료와 함께 커넥션을 반환한다.


### `@Transactional` 없이 DB 작업하면?

```java
public void saveWithoutTransaction() {     
    productRepository.save(product);  // 자체 트랜잭션으로 커밋됨     
    otherRepository.save(other);      // 별도 트랜잭션 
}
```

- 각 저장 작업은 독립된 트랜잭션으로 실행되어 원자성이 보장되지 않는다.

- 중간에 에러가 나도 앞선 작업은 롤백되지 않는다.


### 트랜잭션과 동시성 제어

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateStock(Long productId, int quantity) {
    Product product = productRepository.findById(productId);
    product.updateStock(quantity);
}
```

- 트랜잭션 격리 수준에 따라 동시성 문제(Dirty Read, Non-repeatable Read 등)를 제어할 수 있다.
- 기본값은 DB의 기본 격리 수준을 따른다 (대부분 READ_COMMITTED).

---

## 3. 실전에서의 주의사항

### 외부 API와 트랜잭션

```java
@Transactional 
public ProductDto createProduct(ProductRequest req) {     
    Product p = productRepository.save(...); // 커넥션 획득     
    StockResponse s = stockClient.createStock(p.getId()); // 커넥션 점유 상태로 외부 API 호출     
    return new ProductDto(p, s); 
}
```

- 외부 API 호출이 느려지면 커넥션이 장시간 점유되고, 커넥션 풀이 고갈될 수 있다.

**개선된 방법 (트랜잭션 분리)**:

```java
public ProductDto createProduct(ProductRequest req) {     
    Product p = productService.saveProduct(req); // 별도 트랜잭션으로 분리     
    StockResponse s = stockClient.createStock(p.getId()); // 트랜잭션 외부     
    return new ProductDto(p, s); 
}

public Product saveProduct(ProductRequest req) {
    return productRepository.save(Product.from(req));
}
```

- DB 커넥션 점유 시간을 최소화할 수 있다.

- 단, 외부 API 실패 시 데이터 불일치가 발생할 수 있다.


### 읽기 전용 트랜잭션 최적화

```java
@Transactional(readOnly = true)
public List<ProductDto> getProducts() {
    return productRepository.findAll().stream()
        .map(ProductDto::from)
        .collect(Collectors.toList());
}
```

- `readOnly = true`로 설정하면 DB 드라이버 레벨에서 최적화가 가능하다.
    - JPA의 세션 플러시 모드가 MANUAL로 설정된다. (변경감지 적용x)

### 보상 트랜잭션과 Saga 패턴

```java
public ProductDto createProduct(ProductRequest req) {     
    Product p = saveInTx(req);     
    try {         
        stockClient.createStock(p.getId());         
        return new ProductDto(p);     
    } catch (Exception e) {         
        deleteInTx(p.getId());  // 보상 트랜잭션         
        throw new ServiceException("롤백됨");     
    } 
}
```

- 실패 시 데이터 정합성을 맞추기 위한 보상 로직 필요
- 마이크로서비스 환경에서는 Saga 패턴(Orchestration/Choreography) 고려

---

## 4. 자주 하는 실수와 해결책

### Self-Invocation 문제

```java
@Service
public class UserService {
    
    @Transactional
    public void createUserListWithTrans(){
        for (int i = 0; i < 10; i++) {
            createUser(i); // 같은 클래스 내부 호출은 프록시를 거치지 않음
        }
    }

    @Transactional
    public User createUser(int index){
        User user = User.builder()
                .name("testname::"+index)
                .email("testemail::"+index)
                .build();
        
        userRepository.save(user);
        return user;
    }
}
```

1. **프록시 기반 AOP의 제약사항**
    - "Due to the proxy-based nature of Spring's AOP framework, calls within the target object are, by definition, not intercepted."
      같은 객체 내부에서의 메서드 호출은 **절대 인터셉트되지 않습니다**. (프록시 기반이므로)

**올바른 해결책들**

#### 1. 클래스 분리 (가장 권장)

```java
@Service
public class UserService {
    private final UserTransactionService userTransactionService;
    
    public void createUserListWithTrans() {
        for (int i = 0; i < 10; i++) {
            userTransactionService.createUser(i); // 외부 호출 = 프록시 거침
        }
    }
}

@Service
public class UserTransactionService {
    private final UserRepository userRepository;
    
    @Transactional
    public User createUser(int index) {
        return userRepository.save(User.builder()
            .name("testname::" + index)
            .email("testemail::" + index)
            .build());
    }
}
```

#### 2. Self-injection (권장X)

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserService self; // 자기 자신을 주입
    
    public UserService(UserRepository userRepository, @Lazy UserService self) {
        this.userRepository = userRepository;
        this.self = self;
    }
    
    @Transactional
    public void createUserListWithTrans() {
        for (int i = 0; i < 10; i++) {
            self.createUser(i); // 프록시를 통한 호출
        }
    }
    
    @Transactional
    public User createUser(int index) {
        return userRepository.save(User.builder()
            .name("testname::" + index)
            .email("testemail::" + index)
            .build());
    }
}
```

### 예외와 롤백

- 모든 예외가 롤백을 트리거하지는 않는다

```java
@Transactional  // 기본적으로 RuntimeException과 Error만 롤백
public void saveWithRollback() {
    try {
        // DB 작업
    } catch (Exception e) {
        throw new RuntimeException(e);  // 롤백됨
    }
}

@Transactional
public void saveWithoutRollback() throws Exception {
    // DB 작업
    throw new Exception("오류 발생");  // 롤백되지 않음 (Checked Exception)
}
```

해결책

```java
@Transactional(rollbackFor = Exception.class)  // 모든 예외에 대해 롤백
public void saveWithRollbackForAllExceptions() throws Exception {
    // DB 작업
}
```
### 트랜잭션 경계와 LazyInitializationException

```java
//@Transactional 없는경우
public ProductDto getProduct(Long id) {
    Product product = productRepository.findById(id); //연관객체를 Lazy로 설정시
    return ProductDto.from(product); // LazyInitializationException 발생 가능
}
```

- 트랜잭션 밖에서 지연 로딩된 엔티티에 접근하면 예외 발생
- `@Transactional(readOnly = true)` 추가하거나 FETCH JOIN 사용

---
추가 참고 . JPA transactional 의 조회성능 저하 사례

[카카오페이 온라인 결제에서 결정한 Transactional 사용 방식](https://tech.kakaopay.com/post/jpa-transactional-bri/#%EC%B9%B4%EC%B9%B4%EC%98%A4%ED%8E%98%EC%9D%B4-%EC%98%A8%EB%9D%BC%EC%9D%B8-%EA%B2%B0%EC%A0%9C%EC%97%90%EC%84%9C-%EA%B2%B0%EC%A0%95%ED%95%9C-transactional-%EC%82%AC%EC%9A%A9-%EB%B0%A9%EC%8B%9D)

- transactional이 필요 없는 구간은 최대한 사용하지 않는다.
- transactional 사용 구간 안에 3rd party api가 끼지 않도록 persistence layer 바깥에서는 transactional을 사용하지 않는다.
- 의도치 않은 transaction 설정을 피하기 위해 class level에서의 transactional 설정은 하지 않는다.


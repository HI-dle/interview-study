# Redis - LettuceLock
Lettuce: redis를 활용하여 분산락 구현할 때(동시성 문제를 해결할 때) 사용하는 대표적인 라이브러리 중 하나다.

- setnx 명령어를 활용해 분산락 구현
  > setnx(set if not exist) 명령어
  >
  > 키와 밸류를 set할 때 기존의 값이 없을 때만 set하는 명령어
- spin lock 방식
  - setnx를 활용하는 방식
  - retry로직을 개발자가 작성해주어야한다.
    > 방식
    >
    > 1. 스레드 1이 키가 1인 데이터를 레디스에 셋하려고 한다.
    > 2. 레디스에 키가 1인 데이터가 없기 때문에 정상적으로 셋하게 되고 Thread1의 성공 리턴
    > 3. Thread2가 똑같이 키가 1인 데이터를 셋하려 할 때 레디스에 이미 키가 있어 실패 리턴
    > 4. Thread2가 락 획득에 실패를 하였기 때문에 일정 시간 이후에 락 획득을 재시도
         >  - 락 획득할 때까지 재시도를 하는 로직을 작성해 줘야 한다.
- 장점
  - 구현이 간단
  - 스프링 데이터 레디스를 사용하면 lettuce는 기본 라이브러리다.
- 단점
  - 스피드락 방식이므로 동시에 많은 스레드가 락 획득대기 상태라면 레디스에 부하가 있을 수 있다.
    - sleep으로 락 획득 재시도에 텀을 준다.

## spin lock

> spin lock 방식: 락을 획득하려는 스레드가 락이 해제될 때까지 계속 루프를 돌며(lock을 얻으려고) 기다리는 방식
-  장점
  - 락 점유 시간이 짧을 때는 문맥 전환(context switch) 비용이 없음 
  - 커널 호출 없이 유저 스페이스에서 락 처리 가능 → 속도 빠름
- 단점 
  - 락이 오래 점유되면 CPU를 낭비 (다른 작업 못함)
  - 멀티코어 환경이 아니면 효과 미미
- 언제 쓰나?
  - 임계 구역이 매우 짧고 빠르게 끝날 때
  - 컨텍스트 스위치가 오히려 더 비쌀 때
  - 예: 커널, 드라이버, 고성능 멀티스레드 라이브러리 등

## 코드
```java
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis의 명령어를 이용하기 위한 RedisLockRepository
 */
@Component
@RequiredArgsConstructor
public class RedisLockRepository {
  // 레디스의 명령어를 실행을 위한 레디스 템플릿을 변수
  private final RedisTemplate<String, String> redisTemplate;

  /**
   * 로직 실행 전 key와 setnx 명령어를 활용해서 락
   * 키에는 stockID를 넣어줄 것이고 value는 lock이라는 문자를 넣어줄 것이다.
   * @param key 키에 사용할 변수
   * @return 성공,실패
   */
  public Boolean lock(Long key) {
    return redisTemplate
        .opsForValue()
        .setIfAbsent(generateKey(key), "lock", Duration.ofMillis(3_000));
  }

  /**
   * 로직이 끝나면 unlock
   * @param key
   * @return
   */
  public Boolean unlock(Long key) {
    return redisTemplate.delete(generateKey(key));
  }

  private String generateKey(Long key) {
    return key.toString();
  }
}
```

```java
import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.NamedLockStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 로직 실행 전후로 락 획득 해제를 수행하기위한 facade
 */
@Component
@RequiredArgsConstructor
public class LettuceLockStockFacade {

  private final RedisLockRepository redisLockRepository;

  private final NamedLockStockService stockService;

  public void decrease(Long key, Long quantity) throws InterruptedException {
    while (!redisLockRepository.lock(key)) { // lock 획득 시도
      Thread.sleep(100); // 레디스 부하 줄임
    }

    try {
      stockService.decrease(key, quantity);
    } finally {
      redisLockRepository.unlock(key); // lock 해제
    }
  }
}
```

## vs Redisson

| 항목       | **Lettuce Lock**                   | **Redisson Lock**                 |
| -------- | ---------------------------------- | --------------------------------- |
| 사용 라이브러리 | Lettuce (Spring Data Redis 등에서 사용) | Redisson (별도 라이브러리)               |
| 구현 방식    | 직접 구현 필요 (간단한 setnx 기반)            | Redlock 알고리즘 지원, 자체 구현 완비         |
| 기능 지원    | 기본 락만 제공                           | 분산락, 공정락, 재진입락, 읽기/쓰기락, 세마포어 등 다양 |
| 안정성      | 단일 Redis 노드 기준                     | Redis 클러스터 기반 Redlock (더 높은 안정성)  |
| 자동 갱신    | ❌ (직접 구현 필요)                       | ✅ (Watchdog으로 락 자동 연장)            |
| 재진입 가능   | ❌ (직접 처리해야 함)                      | ✅ 지원                              |
| 락 해제 보장  | ❌ (클라이언트 죽으면 락 남을 수 있음)            | ✅ (자동 만료 + 재시도)                   |
| 사용 편의성   | 코드 작성 필요 많음                        | 고수준 API 제공                        |
| 무거움/가벼움  | 가볍고 단순                             | 무겁지만 풍부한 기능                       |



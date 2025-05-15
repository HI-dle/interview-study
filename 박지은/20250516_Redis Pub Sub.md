> 목차
> 
> - [pub/sub(publish/subscribe) 개념 알아보기](https://github.com/HI-dle/interview-study/blob/main/%EB%B0%95%EC%A7%80%EC%9D%80/20250516_Redis%20Pub%20Sub.md#pubsubpublishsubscribe-%EA%B0%9C%EB%85%90-%EC%95%8C%EC%95%84%EB%B3%B4%EA%B8%B0)
>   - [Pub/Sub 패턴 사용하기 위한 네 가지 개념](https://github.com/HI-dle/interview-study/blob/main/%EB%B0%95%EC%A7%80%EC%9D%80/20250516_Redis%20Pub%20Sub.md#pubsub-%ED%8C%A8%ED%84%B4-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-%EC%9C%84%ED%95%9C-%EB%84%A4-%EA%B0%80%EC%A7%80-%EA%B0%9C%EB%85%90)
>   - [vs 메시지 큐](https://github.com/HI-dle/interview-study/blob/main/%EB%B0%95%EC%A7%80%EC%9D%80/20250516_Redis%20Pub%20Sub.md#vs-%EB%A9%94%EC%8B%9C%EC%A7%80-%ED%81%90)
>   - [vs Observer(관찰자) Pattern](https://github.com/HI-dle/interview-study/blob/main/%EB%B0%95%EC%A7%80%EC%9D%80/20250516_Redis%20Pub%20Sub.md#vs-observer%EA%B4%80%EC%B0%B0%EC%9E%90-pattern)
> - [Redis의 Pub/Sub](https://github.com/HI-dle/interview-study/blob/main/%EB%B0%95%EC%A7%80%EC%9D%80/20250516_Redis%20Pub%20Sub.md#redis%EC%9D%98-pubsub)
> - [Redisson](https://github.com/HI-dle/interview-study/blob/main/%EB%B0%95%EC%A7%80%EC%9D%80/20250516_Redis%20Pub%20Sub.md#redisson)

# Redis Pub/Sub

> RedissonLock 에서는 Pub/Sub 개념을 사용한다. 
> RedissonLock 을 먼저 알아보기 전에 이번에는 Pub/Sub 에 대해서 알아보고자 하려한다.

## pub/sub(publish/subscribe) 개념 알아보기
> pub/sub: 비동기 통신을 수행하기위한 소프트웨어 아키텍처 및 디자인의 핵심 개념
> - 서버리스 및 마이크로 서비스 아키텍처에서 비동기 적으로 통신하기위한 소프트웨어 메시징 패턴

### Pub/Sub 패턴 사용하기 위한 네 가지 개념
- Message: 다른 당사자들 사이에서 교환되도록 인코딩 된 직렬화 된 개별 통신 단위.
- Publisher: 메시지 보내는 응용 프로그램 또는 기타 엔터티.
- Subscriber: 하나 이상의 publisher 로부터 message 를 받는 응용 프로그램 또는 기타 엔터티.
- Topic: 특정 subject에 대한 message 가 포함 된 채널.

Redisson의 pub/sub(Publish/Subscribe) 기능에서 가장 중요한 개념은
**메시지를 보내는 쪽(발행자)**과 **받는 쪽(구독자)**이 서로를 전혀 몰라도 된다는 것이다.

즉, 서로 독립적으로 동작할 수 있다.

> 예를들어, 여러 개의 Spring Boot 서버가 있다고 하고,
> 
> 그중 하나가 어떤 이벤트(예: "상품 재고 소진됨")를 Redisson을 통해 발행(publish)하면,
> 
> 해당 주제를 "구독(subscribe)"한 다른 서버들이 바로 그 메시지를 받아 처리하게 된다.
> 
> 이때,
> - 이벤트를 발행한 서버는 누가 이걸 받을지 신경 쓰지 않는다.
> - 반대로 메시지를 받는 서버는 누가 보냈는지 몰라도 된다.
> 이러한 구조 덕분에 서버들끼리 의존하지 않고도 유기적으로 동작할 수 있게 된다.
> 
> 즉, 한 서버가 메시지를 보내도, 나머지 Spring Boot 서버들은 그냥 "주제"만 보고 알아서 반응하는 구조!

### vs 메시지 큐
| 구분 | pub/sub                     | message queue                 |
| -- | --------------------------- | ----------------------------- |
| 구조 | 발행자가 주제를 보내고, 구독자가 즉시 수신    | 메시지를 큐에 쌓고, 소비자가 가져감          |
| 특징 | 실시간성 / 비동기적 / 발신자-수신자 독립    | 지연 허용 / 순차 처리 용이              |
| 비유 | Spring Boot 서버 간 실시간 이벤트 전달 | Spring Boot 서버가 큐에서 순서대로 일 처리 |

### vs Observer(관찰자) Pattern
"관찰자(Observer)" 패턴에서는 하나의 객체(= Subject, 또는 Observable)가 자신을 **관찰하는 여러 객체(= Observers)**를 알고 있다.
- 이 Subject가 어떤 변화를 겪으면, 자신이 등록해둔 Observer들에게 직접 알려줘서 상태를 업데이트하게 만든다.

| 항목     | Observer Pattern          | Pub/Sub Pattern                  |
|--------|---------------------------|----------------------------------|
| 관계     | 1:Many (하나의 주체 → 여러 관찰자)  | Many\:Many (여러 발행자 ↔ 여러 구독자)     |
| 결합도    | 서로 알아야 함 (강한 결합)          | 서로 몰라도 됨 (느슨한 결합)                |
| 예시     | 재고 서버가 직접 관찰자 서버들에게 알림    | 결제 서버가 메시지를 주제에 발행, 배송 서버는 구독 중  |
| 메시지 흐름 | 주체 → 관찰자 목록 직접 순회하며 전달    | 주제에 메시지를 발행하고, 구독자는 주제 기반 수신     |

### 장점
- 효율성 (Efficiency)
  -  발행(publish)되자마자 즉시 구독자(subscriber)에게 전달되기 때문에 실시간 반응이 필요할 때 매우 유리하다.
     - 일반적인 방식에서는, 수신자가 새로운 메시지가 있는지 계속 확인(polling)하게되어 네트워크/리소스를 낭비하게 된다.
- 확장성 (Scalability)
  - Pub/Sub은 발행자와 구독자가 서로 독립적이어서, 한쪽을 수정하거나 늘려도 다른 쪽에 영향을 주지 않기 때문에, 부분적으로 개선하거나, 여러 서버로 수평 확장이 쉬워진다.
- 단순함 (Simplicity)
  - 서로 직접 연결하지 않아도 되기 때문에 구성이 단순하다.

이러한 장점 덕분에 PUB/SUB 패턴은로드 밸런싱, 이벤트 중심 아키텍처 및 데이터 스트리밍 등에 많이 사용된다.

## Redis의 Pub/Sub
Redis는 메모리 기반의 데이터 저장소로,

키-값 저장 (NoSQL DB), 캐시, 메시지 브로커 등 다양한 용도로 사용된다.

이 중 메시지 브로커 기능을 이용하면, Pub/Sub 패턴을 Redis를 통해 간단히 구현할 수 있다.

### 명령어
- `PUBLISH`: 메시지를 특정 채널에 발행
- `SUBSCRIBE` 명령: 특정 채널을 구독해서 메시지 수신
```redis
-- sports라는 채널에 "oilers/7:leafs/1" 메시지를 발행
PUBLISH sports "oilers/7:leafs/1" 

SUBSCRIBE sports 
```

Redis는 명령어 기반이라, Java 같은 환경에서는 호환되지 않아 바로 사용하기 불편하다.
코드로 사용하려면, 별도의 Redis 클라이언트 라이브러리가 필요하다.

## Redisson
Redisson은 Java/Spring Boot에서 Redis를 쉽게 사용할 수 있게 도와주는 라이브러리다.

특히, RTopic이라는 기능을 제공해서 Pub/Sub을 Java 코드로 쉽게 구현할 수 있게 해준다.

```java
// 메시지 수신자 (구독자)
RTopic topic = redisson.getTopic("anyTopic");

topic.addListener(SomeObject.class, new MessageListener<SomeObject>() {
    @Override
    public void onMessage(String channel, SomeObject message) {
        // 메시지 수신 시 처리할 로직
    }
});
```
```java
// 메시지 발행자 (퍼블리셔) - 다른 스레드 또는 다른 서버에서
RTopic topic = redisson.getTopic("anyTopic");
topic.publish(new SomeObject());
```
anyTopic이라는 채널에 메시지를 발행하면, 해당 채널을 구독 중인 모든 리스너가 onMessage()를 통해 바로 메시지를 받는다.

### Redisson 특징

| 기능        | 설명                                                           |
|-----------|--------------------------------------------------------------|
| 쉬운 사용법    | Java에서 Redis Pub/Sub을 코드로 쉽게 구현 가능                           |
| 자동 재구독    | Redis가 장애나 failover가 발생해도, **리스너가 자동으로 다시 구독**함              |
| 다양한 방식 지원 | 동기/비동기/Reactive 방식까지 모두 지원 (`RxJava`, `CompletableFuture` 등) |


> 참고
> - [redisson](https://redisson.pro/glossary/pubsub.html)
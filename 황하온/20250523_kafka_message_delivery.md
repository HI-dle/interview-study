# Kafka Message Delivery Semantics 메세지 전달 보장
Kafka에서 메시지 전달 보장 수준은 3단계로 나눌 수 있다.

용어 | 설명
--- | ---
No gurantee |	메세지 전송에 대해 보장하지 않는다. <br />Producer 가 보낸 메세지가 유실되거나 Consumer 는 한번 또는 여러번 동일한 메세지를 처리할 수 있다
At most once | 메세지를 최대 단 한 번 전송 처리할 것을 보장한다. <br />단 이때 중복 가능성을 피하기 위해 메세지가 전송되지 않을 수도 있다.
At least once	| 메세지를 최소 한 번 전송 처리할 것을 보장한다. <br />재전송이 발생할 수 있으므로, Consumer는 한 번 이상 메세지를 처리할 수 있다.
Exactly once | 메시지는 유실되지 않고 정확히 단 한 번 전송이 완료된다. <br />재전송이 일어나지 않는다.

## Producer Message Semantic 프로듀서 관점

Kafka Broker 에게 메세지 토픽을 전달한 이후, Offset Storage 저장 방식 및 Producer와 Broker 사이의 ack 응답 처리와 관계가 있다.

#### acks 옵션
- acks = 0
  - 메시지를 보낸 후에 정상적으로 처리되었는지 확인하지 않는다.
  - 처리 속도는 다른 옵션들이 비해 빠르지만 유실 가능성이 있다.
- acks = 1
  - 메시지를 보낸 후에 leader 파티션션에게 메시지가 잘 전달되었는지 확인한다.
  - leader는 로그 파일에 추가하고 다른 follower의 복제 완료를 기다리지 않고 응답한다.
  - leader 가 메시지를 받은 것을 보장한다. 그러나 follower 들로 복제가 되기 전에 leader 브로커에 장애가 발생한다면 유실 가능성이 있다.
- acks = all(-1)
  - leader와 ISR Follower들이 메시지를 모두 전달 받았는지, 즉 복제 완료되었을 경우 전송 완료로 체크한다.
  - min.insync.replicas 수(= leader + follower)만큼 응답이 와야 한다. 
  - 처리 시간 등은 오래걸리고 성능이 제일 느리지만 유실 가능성이 적고, 월등한 안정성을 제공함.
  - kafka 3.0 이상에서는 acks=all 이 default 로 설정된다
    - 데브 원영님 왈, 동기 처리하는 게 아니라면 딱히 성능에 영향이 없다고 하며 all 설정을 권장한다.

### At most once   
메시지를 한 번 전송하면 끝이기 때문에 따로 전송 결과에 대한 응답을 기다릴 필요가 없다.   
따라서 메시지가 다소 유실되어도 괜찮고 대신 더 높은 성능을 요구하는 상황이라면 At most once를 고려해 볼만 하다.
- acks = 0 설정에 해당한다.

### At least once
메시지는 절대 유실되지 않고 최소 한 번 이상 전송이 완료된다.  
메시지를 전송한 후 해당 메시지가 타겟 파티션에 저장되었다는 응답을 확인한다. 만약 실패한 경우 메시지를 다시 전송한다.  
이 과정에서 브로커의 문제, 네트워크 문제 등으로 ack 응답이 유실되는 경우, 부득이하게 재전송이 일어날 수 있다.

### Exactly once

Exactly Once 는 메세지의 Broker 전달과 Offset Storage 저장을 Atomic 하게 보장하는 것을 의미한다.  
 
- Exactly-Once 보장이 필요한 대표적인 사례로는 금융 거래나 결제 시스템을 들 수 있을 것이다.
  - 성능 오버헤드가 있으므로 꼭 필요한 기능에서만 활용하는 것이 권장된다.

#### 메세지 유실 가능성?
- 그러나 여전히 메세지 유실 혹은 발행 실패(카프카 서버 다운 등이 발생하여 재처리도 불가) 문제가 발생할 수 있을 것 같다.
  - 이러한 경우, 로그를 남겨서 서버가 안정화 된 다음, 재처리하는 방안을 생각해 볼 수 있을 것이나, 이를 자동화 하기 위해서 추가로 기능을 구현해야 하는 문제가 생길 수 있다.
  - **아웃박스 패턴**을 활용한다면, 이러한 문제에서 조금 더 자유로울 수 있을 것 같다. + 이런 경우 굳이 Exactly once를 고수할 이유 없이 **At Least Once 방식을 함께 활용**하도록 해서 성능 문제에서도 벗어날 수 있다.


#### 멱등성 프로듀서
![image](https://github.com/user-attachments/assets/8bbd24a3-f4b1-462b-9377-514dc323b760)

- 카프카 0.11.0.0 버전 이전에는 
  - 전송한 메시지의 저장 실패 응답을 받은 경우, 메시지를 재전송하는 것 외 선택지가 없었다. 즉, At least once 까지만 보장할 수 있었다. 
- 0.11 버전 이후 
  - 메시지 전송을 **멱등적(idempotency**)으로 수행할 수 있게 되었다.   
  - 브로커는 **프로듀서에 ID**를 부여하고 거기서 보내는 메시지에는 **시퀀스 번호**를 붙여 **중복으로 전송된 메시지를 무시하고, ack 신호만** 보낸다.

```
# 해당 설정을 세팅한다.
enable.idempotence=true

# 이 경우 아래의 설정에 제약이 생기며, 충족하지 못하면 오류가 발생한다

acks = all
retries는 0 보다 큰 값
max.in.flight.requests.per.connection은 1에서 5사이 값 (기본값 5)
```
- max.in.flight.requests.per.connection : 비동기 전송 시 브로커의 응답없이 한꺼번에 보낼 수 있는 Batch의 개수를 의미한다.
  - 이 수가 크면, 메세지의 순서를 보장하기 어려워진다.

#### 트랜잭션 프로듀서
- 또한 0.11 부터 여러 토픽에 메시지를 보내는 과정을 **트랜잭션화**하여 전부 성공하거나 혹은 모두  실패하는 원자적(atomicity) 실행을 보장할 수 있게 되었다.

![image](https://github.com/user-attachments/assets/64f008b0-9f2e-41ab-9de6-1ca4ecc1c0ef)

```bash
# 멱등성 프로듀서 설정을 true로 하고 트랜잭셔널 아이디 값을 적절히 설정한다.
transactional.id
```

- 트랜잭션 프로듀서는 사용자가 보낸 데이터를 레코드로 파티션에 저장할 뿐만 아니라 트랜잭션의 시작과 끝을 표현하기 위해 트랜잭션 레코드를 한 개 더 보낸다. (트랜잭션 레코드는 오프셋을 한 개 차지한다.)

- 트랜잭션 컨슈머는 파티션에 저장된 트랜잭션 레코드를 보고 트랜잭션이 완료되었음을 확인하고 데이터를 가져간다.


## Consumer Message Semantics 컨슈머 관점
Kafka에서는 컨슈머가 메시지를 어디까지 읽었는지 오프셋의 위치를 내부 토픽인 __consumer_offsets에 기록한다.  
- 이 오프셋 커밋을 수행하는 시점에 따라 보장 정도가 달라진다.

Property | Description | Guarantee
--- | --- | ---
enable.auto.commit = true	| 일정 주기(auto.commit.interval.ms, default 5초)마다 poll 한 record 의 offset 을 commit 하는 전략 | At Most Once
enable.auto.commit = false | application 단에서 수동으로 commit 하는 전략 | At Least Once 


### At most once   
![image](https://github.com/user-attachments/assets/5d0c6dfd-31bc-47fa-ace4-6d77815fe26d)

컨슈머가 메시지를 읽은 후 오프셋을 먼저 기록한다. 그리고 메시지를 처리한다.  
메시지 처리에 실패하는 경우에도 오프셋 커밋이 이미 수행되어, 처리 실패한 메시지를 넘기고 다음 메시지를 읽게 된다. 따라서 메시지가 유실되는 문제가 발생할 수 있다.

### At Least Once
![image](https://github.com/user-attachments/assets/d3fa983e-ee14-4e0c-8670-cea53b70bd14)

컨슈머가 메시지를 읽고 모든 처리를 성공적으로 마쳤다면 오프셋을 기록한다.  
그러나 메시지 처리를 완료한 후 로그에 오프셋을 기록하기 직전, 컨슈머가 장애를 일으켜 재시작할 수 있다.   
  - 이런 경우, 오프셋 기록되지 않았으므로, 컨슈머가 재시작 된 후 혹은 리밸런싱이 발생한 이후 기록된 오프셋부터 메시지를 읽어 처리하게 되어 중복 처리가 발생할 수 있다.  

### Exactly Once

컨슈머는 1) 메시지 처리와 2) 메시지 오프셋 기록하기 총 2가지 일을 해야 한다.   
Exactly Once를 보장하기 위해 이 2개의 작업을 하나의 트랜잭션으로 묶어서 둘 중 하나가 실패한 경우 전부 롤백하고 두 가지가 모두 성공한 경우에만 성공으로 처리할 수 있다.   
- 이 때 메시지 처리 과정이 구체적으로 무엇이냐에 따라 처리 방안이 달라질 수 있다.

#### 메시지 처리 후 다른 토픽에 쓰는 경우
특수한 상황으로 메시지를 소비한 후 바로 다른 토픽으로 발행하는 상황이다. 즉 메시지 처리 동작이 곧 메시지 발행인 경우다.   

이 과정을 처리하기 위해 위에서 언급한 0.11 버전에서 추가된 여러 토픽에 대한 메시지 전송을 **트랜잭션화**하는 기능을 사용할 수 있다.   
즉, 오프셋을 기록하기 위해 내부 토픽인 __consumer_offsets에 메시지를 발행하는 작업과 컨슈머의 메시지 처리 결과를 다른 토픽에 쓰는 과정을 하나의 트랜잭션으로 묶는다.  
 이 두 가지 작업을 트랜잭션으로 묶으면 둘 중 하나가 실패하여 전부 롤백되거나 둘이 함께 성공하기 때문에 Exactly Once를 보장할 수 있다.   

```bash
# 프로듀서 팩토리에 transation.id를 임의의 문자열 값으로 설정한 후, KafkaTemplate의 executeInTransaction 메서드를 호출하여 트랜잭션을 시작

transactional.id
```

- 혹은 이러한 과정에 Kafka Streams를 활용할 수 있다. 토픽에서 다른 토픽으로 메시지를 처리하고 이동하는 과정을 만드는데 유용하다고 한다.

#### 메시지 처리 후 외부 저장소에 쓰는 경우
컨슈머의 메시지 처리 과정이 외부 시스템에 데이터를 쓰는 상황이 있다.(보편적인 케이스) 

이 경우 고전적인 방법은 **two-phase commit**을 사용할 수 있다. 하지만 two-phase commit을 서로 다른 저장소 (Kafka의 __consumer_offsets 파티션, 외부 저장소)에 대해 구현하는 것이 쉽지 않으므로 공식문서에서는 컨슈머의 오프셋을 외부 저장소에 함께 저장하는 것을 권한다. <- **인박스(Inbox) 패턴** 과 유사 
- 예를 들면 컨슈머가 MySQL이라면 컨슈머 오프셋을 저장하는 테이블을 따로 생성한다. 
- 그 후 처리한 레코드를 저장하는 쿼리와 컨슈머 오프셋을 업데이트하는 쿼리를 MySQL의 트랜잭션으로 묶어서 처리한다. 
- 이 경우 two-phase commit을 지원하지 않더라도 오프셋 업데이트와 메시지 처리는 동시에 성공하거나 실패함을 보장하고, 외부 저장소에 저장된 컨슈머 오프셋 번호를 보고 메시지를 가져올 수 있고 가져온 메시지가 중복인지 확인할 수도 있다. 
- 공식문서 예시로 Kafka Connect 커넥터를 사용하여 데이터 저장과 오프셋 업데이트를 트랜잭션화 하여 처리하는 것을 알려준다고 한다.

![image](https://github.com/user-attachments/assets/84ba8937-50f6-4951-88a0-58171763d167)

- 이미지와 같이 디비에 중복 방지 키를 저장해두고, **중복 처리인지 체크하는 로직을 어플리케이션 레벨에서 구현할 필요성**이 있다.
  - **인박스 패턴**

#### 추가) 컨슈머 격리 수준 설정

- 트랜잭션 프로듀서와 연관된 설정
  - isolation.level=read_committed 설정(default는 read_uncommited)으로, 커밋 완료된 트랜잭션의 메시지만 읽게 되어 트랜잭션이 불완전한 동안의 중간 결과는 필터링하게 할 수 있다.


### Kafka Streams
Kafka Streams의 경우 다음 옵션을 지정하면 Exactly Once를 쉽게 이룰 수 있다고 한다.
```
processing.guarantee=exactly_once
```

### 참고자료
- https://easywritten.com/post/kafka-message-delivery-semantics/
- https://huisam.tistory.com/entry/kafka-message-semantics
- https://velog.io/@xogml951/Kafka%EC%99%80-Exactly-Once

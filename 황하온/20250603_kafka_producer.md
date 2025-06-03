# 카프카 프로듀서 Client
![image](https://github.com/user-attachments/assets/505ca1c7-156a-40ce-b98e-33e918d094c3)

- 메시지가 브로커로 전달되는 과정 (출처 : https://www.linkedin.com/pulse/kafka-producer-overview-sylvester-daniel)

카프카 프로듀서는 내부적으로 직렬화, 파티셔닝, 배치 생성, 압축의 단계를 거쳐 브로커로 데이터를 전송하게 된다.

## Serializer
![image](https://github.com/user-attachments/assets/50fda693-0f1e-42c6-b338-1499ce729054)

카프카 프로듀서는 Serializer를 통해 데이터를 Byte Array 형태로 직렬화하여 전송하고, 파티션에서도 Byte Array 형태로 저장하게 된다.  
카프카 컨슈머에서는 Byte Array로 직렬화 된 데이터를 받아서 DeSerialization 과정을 거쳐 원본 객체로 변환한다.

## Partitioner
카프카 파티셔너는 ProducerRecord(카프카 이벤트 객체)를 토픽의 어느 파티션으로 전송할 지 결정한다.  
RoundRobinPartitioner, UniformStickyPartitioner를 제공한다.

### 메시지 키
- 카프카는 하나의 파티션 내에서만 메시지 순서를 보장하기 때문에 메시지 순서를 보장하기 위해서는 메시지 키를 사용해야 한다.
  - Key의 해시값(MurmurHash2 Algorithm)으로 파티션이 결정되므로, 같은 키를 가지는 메세지는 같은 파티션으로 전송된다.
- 메시지는 Partitioner를 통해 토픽의 어떤 파티션으로 전송되어야 할지 결정 되는데, Key값을 가지지 않는 경우 라운드 로빈(Round Robin), 스티키 파티션(Sticky Partition) 등의 지정된 파티션 전략을 통해 메시지가 전송된다.

### RoundRobinPartitioner
카프카 클라이언트 2.3 이하 버전에서 기본 파티셔너이다.   
- 메시지 키가 있으면 키의 해시값으로 파티션을 매칭한다.  
- ProducerRecord가 들어오는 대로 파티션을 순회하면서 전송하기 때문에 배치로 묶이는 빈도가 적다.
  - 배치 기능을 비효율적으로 사용하기 때문에 레이턴시가 늘어가게 된다.

### UniformStickyPartitioner
카프카 클라이언트 2.4 이후 버전에서 기본 파티셔너이다.
- 메시지 키가 있으면 키의 해시값으로 파티션을 매칭한다.
- 배치 크기(batch.size)를 초과하지 않는 수준에서 같은 파티션으로 보내는 레코드는 최대한 묶어서 전송한다. 
  - 결국 모든 파티션을 순회하게 되어 있기 때문에 공정하게 분배되고고 성능은 RoundRobinPartitioner보다 좋다.
  ![image](https://github.com/user-attachments/assets/1f31028f-6330-4b11-9d41-1be69415f1b7)

## Accumulator
![image](https://github.com/user-attachments/assets/4cd6e23e-58ff-4de5-9ca3-7e92d13027c5)

Record Accumulator는 Partitioner에 의해 지정되는 메시지 배치가 전송이 될 **topic과 Partition**에 따라 할당받는 KafkaProducer 메모리 영역이다.
 
- KafkaProducer 객체의 send() 메소드는 호출될 때 하나의 ProducerRecord를 입력하지만, 하나의 데이터가 바로 전송 되지 않는다.
-  Accumulator에서 topic, partition에 대응하는 배치 큐(Batch Queue)를 구성하고 메시지들을 레코드 배치(Record Batch) 형태로 묶어 해당큐에 저장한다.
- Accumulator에서 여러 메시지 Batch들은 buffer.memory 설정 사이즈 만큼 보관될 수 있다. 
- 메시지는 Sender Thread에 의해 여러 개의 Batch들로 한꺼번에 전송될 수 있다.
  - Sender Thread는 Accumulator에 누적된 메시지 배치를 꺼내서 브로커로 전송하게 되는데, 이 때 1개의 Batch를 가져갈수도, 여러 개의 Batch를 가져 갈 수도 있다.
 ![image](https://github.com/user-attachments/assets/c62169e5-f38d-47f0-be6b-05474dc88291)

    - Sender 스레드는 네트워크 비용을 줄이기 위해 piggyback 방식으로 조건을 만족하지 않은 다른 레코드 배치를 조건을 만족한 것과 함께 브로커로 전송하게 된다.

####  Piggyback이란 
- '등 뒤에 업다'라는 뜻을 갖는다. 
- 위 그림을 예로 들어 토픽 B의 파티션 1(B_1)의 큐에 레코드 배치가 전송할 조건을 만족했다고 가정한다.
  - Sender는 해당 레코드 배치를 가져와 3번 브로커로 전송할 준비를 한다. 이때, 토픽 A의 파티션 2(A_2)가 전송 조건을 만족하지 않았더라도 같은 3번 브로커에 전송돼야 하므로, Sender는 A_2 레코드 배치를 업어 한번에 3번 브로커로 전송할 수 있다. 
  - 이러한 방식으로 자연스럽게 네트워크 비용을 줄일 수 있다.

## Compression
메세지 배치를 압축하여 전송하도록 설정할 수 있다.  
아래는 제공되는 설정과 옵션에 대한 간단한 비교 설명이다.
```
compression.type: none(default)
```

| 타입       | 장점          | 단점     | 비고            |
| -------- | ----------- | ------ | ------------- |
| `none`   | 빠름          | 데이터 큼  | 압축 안 함        |
| `gzip`   | 높은 압축률      | 느림     | CPU 사용량 많음    |
| `snappy` | 빠름          | 중간 압축률 | 구글에서 만든 성능 균형 |
| `lz4`    | 매우 빠름       | 낮은 압축률 | 실시간 처리에 적합    |
| `zstd`   | 높은 압축률 + 빠름 | 비교적 최신 | Kafka 2.1+ 권장 |


## Sender
![image](https://github.com/user-attachments/assets/d0b9cc32-ee54-4917-aba5-ce36e62edadb)
- Producer Client의 Thread가 send() 메소드를 호출하면 메시지 전송 작업을 시작은 하지만, 실제로 바로 전송되지 않는다. 내부 Buffer에 메시지를 저장 후 별도의 Sender Thread가 Kafka Broker에 실제 전송을 하는 방식으로 동작하게 된다.
- Sender는 Java IO Multiplexing을 이용하여 전송을 위한 별도의 스레드를 생성하지 않아도 돼서 성능을 크게 향상시킨다.

### acks 옵션
![image](https://github.com/user-attachments/assets/64e316f2-ef5a-4597-b339-d65dd31bfc5f)
acks 옵션을 통해 프로듀서가 메시지를 전송하고 다음 메시지를 전송하기 전에 브로커로부터 데이터 수신 응답을 기다릴지 설정할 수 있다.  
- 기본값은 -1(all)이다.

#### acks = 0
![image](https://github.com/user-attachments/assets/b0fb6783-05cc-4b56-a95c-0867d84d1b96)
- 리더 브로커가 메시지를 수신했는지에 대한 Ack 응답을 받지 않고 다음 메시지를 바로 전송한다.
- 메시지 손실의 우려가 가장 크지만 가장 빠르게 전송할 수 있다.

#### acks = 1 
![image](https://github.com/user-attachments/assets/5da2f508-384d-4828-a952-3028cd7e8960)
- 리더 브로커가 메시지를 수신했는지에 대한 Ack 응답를 받은 후에 다음 메시지를 전송한다.
- 메시지 복제 작업 중 장애가 발생할 경우 다음 리더가 될 브로커에 특정 메시지가 없을 수 있기 때문에 메시지를 소실할 우려가 있다.

#### acks = all, -1
![image](https://github.com/user-attachments/assets/dd18fbee-ee62-40f4-aed3-0aa884ac3409)
- min.insync.replicas(replication factor 3인 경우 2로 권장됨)로 지정된 브로커 수(리더 + ISR 팔로워)만큼 복제 작업의 수행이 완료된 것을 확인한 다음, 리더 브로커가 Ack 응답을 프로듀서로 보낸다.
- 이후 다음 메시지를 전송할 수 있다. 만약 오류 메시지를 브로커로부터 받게 되면 메시지를 재전송하게 된다.
 
#### ISR(In-Sync-Replicas) 
- 현재 리플리케이션이 되고 있는 리플리케이션 그룹(replication group)을 의미한다.
- 리더 파티션과 팔로워 파티션이 모두 동기화 완료된 상태임을 보장한다. 
  - 리더 파티션의 오프셋 개수와 팔로워 파티션의 오프셋 개수가 동일하다면 ISR이라고 볼 수 있다. 
  - ISR 상태가 되어야 온전히 Fail over가 가능해진진다.
- 리더 파티션은 팔로워 파티션들이 주기적으로 데이터 확인을 하고 있는지 검증한다. 만약 설정된 주기(replica.lag.time.max.ms)만큼 확인 요청이 오지 않으면, 해당 팔로워는 더 이상 리더의 역할을 할 수 없다고 판단해 ISR 그룹에서 추방시킨다.

### 그 외 메시지 전송 관련 파라미터
![image](https://github.com/user-attachments/assets/42b7ee18-82e1-4f52-b1b4-c46ab199d4a7)
![image](https://github.com/user-attachments/assets/21ed60aa-bfb2-4dca-989b-5ea1e9e89aff)

#### Accumulator 관련 옵션
```yml
buffer.memory: Record accumulator의 전체 메모리 사이즈, 기본값은 33,554,432 (32MB)
batch.size: 단일 배치의 사이즈, 기본값은 16,384 (16KB)
max.block.ms: send() 호출 시 Record Accumulator에서 버퍼에 입력(append)하지 못하고 block되는 최대 시간, 데이터의 생성 속도가 너무 빨라서 버퍼의 반환이 이루어지지 않으면 새로운 버퍼를 획득하기 위해 대기 시간이 소요된다고 한다. 초과시 Timeout Exception 발생
linger.ms: Sender Thread가 메시지를 보내기 전 메시지를 배치로 만들기 위한 최대 대기 시간, default는 0
```
- 전반적인 Producer와 Broker간의 전송이 느리다면 linger.ms를 0보다 크게 설정하여 Sender Thread가 일정 시간 대기하여 Record Batch에 메시지를 보다 많이 채울 수 있도록 적용할 수 있다.
- linger.ms는 보통 20ms 이하로 설정을 권장한다.

#### Sender 관련 옵션
- retries: 재시도 횟수를 설정, 기본값은 Integer.MAX_VALUE(2147483647), delivery.timeout.ms 시간이 초과되면 예외가 발생하고 종료되므로, 실제로 21억 회를 다 재시도하게 되지는 않는다. 
- request.timeout.ms: 전송에 걸리는 최대 시간. 전송 재시도 대기 시간 제외. 초과시 retry를 하거나 Timout Exception 발생
- retry.backoff.ms: 전송 재시도를 위한 대기 시간.
- delivery.timeout.ms: Producer 메시지 배치 전송에 허용된 최대 시간. 시간이 초과되는 경우 Timeout Exception 발생
  - delivery.timeout.ms >= linger.ms + request.timeout.ms 이어야 한다.
- max.in.flight.requests.per.connection: 비동기 전송시 브로커의 응답 없이 한꺼번에 보낼 수 있는 Batch의 개수, 기본값은 5, 순서 보장이 되지 않을 가능성이 있으며 정확한 순서 보장이 필요한 경우에는 1을 사용하는 것이 권장된다.
- max.request.size: 요청할 수 있는 최대 bytes 사이즈, 거대한 요청을 피하기 위해 프로듀서에 의한 한 배치에 보내는 사이즈를 제한한다. 압축되지 않은 배치의 최대 사이즈이다.

#### In Flight Requests 란 ?
네트워크 IO 를 취급하는 Application에서 In Flight Request 라는 것은 아직까지 응답을 받지 못한 네트워크 요청을 의미한다.  
즉, Kafka Producer 의 관점에서는 Acks를 응답받지 못한 Request를 의미한다.  
그러므로 max.in.flight.requests.per.connection 은 Acks 응답을 받지 않은 In Flight Request 의 갯수 제한을 의미하는 것으로 이해할 수 있다.

## 멱등성(idempotence) 프로듀서
![image](https://github.com/user-attachments/assets/a01eb655-f8a3-4755-8959-491b1560a516)

- 메시지 Header에 Producer ID와 메시지 Sequence를 저장하여 전송한다.
- 브로커는 Producer가 보낸 메시지의 Sequence가 브로커가 가지고 있는 메시지의 Sequence보다 1만큼 큰 경우에만 브로커에 저장하고 브로커에서 메시지 Sequece가 중복 될 경우 이를 메시지 로그에 기록하지 않고 Ack만 전송한다.
  - 시퀀스 넘버가 일정하지 않은 경우에는 OutOfOrderSequenceExceptioin 이 발생한다. 예: 시퀀스 넘버 0 다음에 1이 아닌 2가 오는 경우

### Idempotence 를 위한 Producer 설정
```
enable.idempotence: true
acks: all
retries: 0 보다 큰 값으로 설정 필요
max.in.flight.requests.per.connection: 1에서 5사이 값
```
- Kafka 3.0 버전 부터는 Producer의 기본 설정이 enable.idempotence: true 이다.
- acks=1로 설정하는 등 다른 파라미터를 변경하면 Idempotence 로 동작하지 않지만 메시지는 전송된다.
- enable.idempotence = true 를 명시적으로 설정하고 다른 파라미터를 잘못 설정하면 Config 오류가 발생한다.

## 트랜잭션 프로듀서
![image](https://github.com/user-attachments/assets/870d6aa7-74bd-408f-9713-843c1af0da7f)

트랜잭션 프로듀서는 다수의 파티션에 데이터를 저장할 경우 모든 데이터에 대해 동일한 원자성을 만족시키기 위해 사용된다.
- 트랜잭션 프로듀서는 사용자가 보낸 데이터를 레코드로 파티션에 저장할 뿐만 아니라 트랜잭션의 시작과 끝을 표현하기 위해 트랜잭션 레코드를 한 개 더 보낸다. 
  - 트랜잭션 레코드는 오프셋을 한 개 차지한다.
- 트랜잭션 컨슈머는 파티션에 저장된 트랜잭션 레코드를 보고 트랜잭션이 완료되었음을 확인하고 데이터를 가져간다.

```
# 트랜잭션 프로듀서를 위한 Producer 설정
enable.idempotence = true
transactionl.id 를 임의의 String 값으로 설정

# Consumer 설정정
isolation.level = read_committed
```
### 참고 자료
- https://always-kimkim.tistory.com/entry/kafka101-producer
- https://jinseong-dev.tistory.com/46
- https://devidea.tistory.com/90

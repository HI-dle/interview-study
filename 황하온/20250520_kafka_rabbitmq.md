# 메세지큐 (MessageQueue : MQ)
- 프로세스 또는 프로그램 인스턴스가 데이터를 서로 교환할 때 사용하는 통신 방법. 
- 더 큰 의미로는 메세지 지향 미들웨어(Message Oriented Middleware:MOM)를 구현한 시스템을 의미한다.
- 메시지(데이터)는 End-Point 간에 직접적으로 통신하지 않고 중간에 queue를 통해 중개된다.

## MQ 의 장점
1. 비동기(Asynchronous): queue라는 임시 저장소가 있기 때문에 나중에 처리 가능
2. 낮은 결합도(Decoupling): 애플리케이션 분리 가능
3. 확장성(Scalable): 필요하다면 서비스를 원하는대로 확장할 수 있음
4. 탄력성(Resilience): 일부 서비스가 다운되더라도 전체 흐름이 중단되는 것은 아니며 메시지는 지속하여 MQ에 남아있다.
5. 보장성(Guarantees): MQ에 들어간다면 결국 모든 메시지가 consumer 서비스에게 전달된다는 보장을 제공한다.
6. 중복성 (Redundancy): 실패할 경우 재실행 가능하다.

# RabbitMQ
- 여러 소스에서 스트리밍 데이터를 수집하고 처리를 위해 다른 대상으로 라우팅하는 분산 메시지 브로커
![image](https://github.com/user-attachments/assets/6f871707-bd3e-4bd7-ac2f-592759dec782)
> 이미지 참고 링크 : https://www.cloudamqp.com/blog/part1-rabbitmq-for-beginners-what-is-rabbitmq.html

## 구성 요소
### 메시지(Message)
- RabbitMQ를 통해 전달되는 데이터 단위

### 프로듀서(Producer)
- 메시지를 생성하고 RabbitMQ에 보내는 역할

### 큐(Queue)
- 메시지를 저장하는 장소
  - 메시지는 큐에 저장되었다가 소비자에게 전달된다. 
  - 큐는 기본적으로 FIFO(First In, First Out) 방식으로 메시지를 처리한다.

### 컨슈머(Consumer)
- 큐에서 메시지를 가져와 처리하는 소비자

### 익스체인지(Exchange)
- 메시지를 적절한 큐로 라우팅하는 역할 
  - 프로듀서는 메시지를 익스체인지에 보내며, 익스체인지는 메시지를 적절한 큐로 전달한다.

## RabbitMQ와 AMQP
- RabbitMQ는 **AMQP**(Advanced Message Queuing Protocol)를 구현한다.
- AMQP는 메시지 브로커를 위한 프로토콜로, 메시지의 생성, 전송, 큐잉, 라우팅 등을 표준화하여 메시지 브로커가 상호 운용될 수 있게 한다.
  - 메세지 지향 미드웨어를 위한 개방형 표준 응용계층 프로토콜이다.
    - 즉, 이기종간 메세지 교환의 문제점을 해결하기 위해 등장한 프로토콜이다.

### AMQP의 주요 개념
- **메시지(Message)**: 전송되는 데이터 단위
- **큐(Queue)**: 메시지를 저장하고 전달하는 장소
- **익스체인지(Exchange)**: 메시지를 적절한 큐로 라우팅하는 역할
- **바인딩(Binding)**: 익스체인지와 큐를 연결하는 설정을 말한다. 바인딩을 통해 메시지가 어느 큐로 전달될지 정의한다.

### 익스체인지 유형
- 메시지 브로커가 메시지를 교환기에서 큐로 라우팅하는 방식을 말한다.
- 익스체인지는 다양한 방식으로 메시지를 라우팅할 수 있으며, 주로 메시지의 라우팅 키와 바인딩 키 또는 패턴을 기반으로 작동한다.

1. **Direct Exchange** 
    - 라우팅 키가 정확히 일치하는 큐로 메시지를 전달한다. (Unicast)
    - 예를 들어, 라우팅 키가 error인 메시지는 error라는 바인딩 키를 가진 큐로 전달된다.

2. **Topic Exchange**
    - 라우팅 키의 패턴을 사용하여 메시지를 라우팅한다. (MultiCast)
    - 패턴에는 와일드카드 * (단어 하나)와 # (0개 이상의 단어)가 사용된다.
    - 예를 들어, 라우팅 키가 quick.orange.rabbit인 메시지는 바인딩 키가 *.orange.*인 큐로 전달된다.

3. **Fanout Exchange**
    - 라우팅 키를 무시하고 교환기에 바인딩된 모든 큐로 메시지를 브로드캐스트한다. (BroadCast)
    - 모든 바인딩된 큐로 메시지가 전달된다.

4. **Headers Exchange**
    - 라우팅 키 대신 메시지의 헤더를 기반으로 메시지를 라우팅한다. (MultiCast)
    - 헤더 값과 바인딩된 헤더 값이 일치하는 큐로 메시지를 전달한다.
    - **x-match**: Header 값으로 세팅할 수 있는 파라미터
      - 이 값이 any 인 경우, 헤더 값과 바인딩이 하나라도 일치하면 큐로 메세지를 전달한다.
      - 이 값이 all 인 경우, 헤더 값과 바인딩이 모두 일치해야 해당 큐로 메세지를 전달한다.


# 카프카
![image](https://github.com/user-attachments/assets/004850cd-462e-4fe5-8497-4b5bd9a6fa96)

## 구성 요소
### 메시지(Message)
- 메시지는 Kafka를 통해 전달되는 데이터 단위를 말한다.
- 메시지는 키(key), 값(value), 타임스탬프(timestamp), 그리고 몇 가지 메타데이터로 구성될 수 있다.

### 프로듀서(Producer)
- 메시지를 생성하고 Kafka에 보내는 역할
- 프로듀서는 특정 토픽(topic)에 메시지를 보낸다.

### 토픽(Topic)
- 메시지를 구분/저장하는 논리적 단위를 말한다.
- 토픽은 여러 파티션(partition)으로 나누어질 수 있다. 
  - 파티션을 통해 병렬 처리가 가능하다.

### 파티션(Partition)
- 파티션은 **토픽을 물리적으로 나눈 단위**로, 각 파티션은 독립적으로 메시지를 저장하고 관리한다.
- 각 파티션은 메시지를 순서대로 저장하며, 파티션 내의 메시지는 고유한 **오프셋(offset**)으로 식별된다.
- 파티션을 통해 데이터를 병렬로 처리할 수 있으며, 클러스터 내의 여러 브로커에 분산시켜 저장할 수 있다.

### 키(Key)
- 키는 메시지를 특정 파티션에 할당하는 데 사용되는 값
- **동일한 키를 가진 메시지는 항상 동일한 파티션에 저장**된다.
  - 예를 들어, 특정 사용자 ID를 키로 사용하여 해당 사용자의 모든 이벤트가 동일한 파티션에 저장되도록 할 수 있다.

### 컨슈머(Consumer)
- 토픽에서 메시지를 가져와 처리하는 역할을 수행한다.
- 컨슈머는 특정 **컨슈머 그룹(consumer group)**에 속하며, 같은 그룹에 속한 컨슈머들은 토픽의 파티션을 분산, 병렬 처리한다.
- 기본적으로 컨슈머는 **스티키 파티셔닝(Sticky Partitioning)**을 사용한다. 
  - 이는 특정 컨슈머가 특정 파티션에 붙어서 계속해서 데이터를 처리하는 방식으로, 이는 데이터 지역성을 높여 캐시 히트율을 증가시키고 전반적인 처리 성능을 향상시킨다.

### 브로커(Broker)
- Kafka 클러스터의 각 서버를 의미하며, 메시지를 물리적으로 저장하고 전송하는 역할을 수행한다.
- 하나의 Kafka 클러스터는 여러 브로커로 구성될 수 있으며, 각 브로커는 하나 이상의 토픽 파티션을 관리한다.
- 하나의 컴퓨팅 서버보다는 여러 분산된 서버 환경에 브로커를 띄워서 결함 내성을 갖도록 구성할 수 있다.

### 주키퍼(Zookeeper)
- Kafka 클러스터를 관리하고 조정하는 데 사용되는 분산 코디네이션 서비스
- 주키퍼는 브로커의 메타데이터를 저장하고, 브로커의 상태를 추적 및 관리한다. 
- 브로커 간의 상호작용을 조정(리더 선출 등)한다.

### 참고) 크래프트(Kraft)
- Kafka 2.8.0부터 Zookeeper에 대한 종속성을 없애고, Kafka 자체에서 메타데이터 관리, 리더 브로커 선출 등의 작업을 지원하게 되었다.
- 또한, Kafka 3.3.0에서부터는 Production 환경에서 KRaft를 사용할 수 있게 되었다.


# 비교

| 항목         | RabbitMQ | Kafka |
|--------------|----------|-------|
| 성능               | * 큐가 비어있을 때만 성능이 빠름<br>* 1초에 수백만 개의 메시지 처리 가능하지만 자원이 더 필요 | * 순차적인 disk I/O 방식을 통해 성능 향상<br>* 적은 비용으로 많은 데이터 유지, 1초에 수백만 개의 메시지 처리 가능 |
| 프로토콜     | AMQP, MQTT, STOMP 등의 메시징 플랫폼을 지원함 | 단순한 메시지 헤더를 지닌 TCP 기반 custom 프로토콜을 사용하기 때문에 제어가 어려움 | 
| 라우팅 기능         | * Direct, Fanout, Topic, Headers의 라우팅 옵션을 제공하여 유연한 라우팅이 가능하다<br>* topic exchange를 통해 routing_key 값으로 라우팅 가능<br>* header exchange로 메시지 헤더 기반 라우팅 가능<br>* consumer는 메시지 종류별로 수신 가능 | * 기본 기능으로 라우팅에 대해서 지원하지 않음. Kafka Streams를 활용하여 동적 라우팅을 구현할 수 있다<br>* 소비자가 polling 이전에 토픽에서 메시지를 필터링할 수 없음<br>* 소비자는 예외 없이 파티션의 모든 메시지를 수신해야 함<br>* 필터링이 가능하지만 복잡한 과정 필요 |
| 메시지 보유 및 삭제 | * 소비자가 메시지를 소비하면 ACK 메시지를 보내고 삭제됨<br>* NACK 시 메시지 재전송 가능하지만 큐에 다시 보냄<br>* message broker 설계 방식에 따라 변경 어려움 | *  메시지를 로그 파일에 추가하며, 메시지는 보존 기간이 만료될 때까지 보관<br>* 저장소 크기와 상관없이 메시지 저장 가능 |
| 메시지 순서 보장    | * 메시지 소비자가 하나라면, 메시지 순서를 보장<br>* 여러 소비자가 있으면 순서 보장 어려움 | * 같은 토픽 파티션으로 보낸 메시지는 순서대로 처리됨을 보장<br>* 같은 토픽 내 여러 개의 파티션 사이에서 처리 순서는 보장하지 않음<br>* 여러 파티션 사이에서 순서를 보장하려면 키를 이용하여 메시지를 그룹화하여 동일한 키는 동일한 파티션으로 이동하게 해야 함 |
| 지연 메시지         | * 생산자는 message exchange에서 큐에 전송되는 메시지 시간을 정할 수 있음 | * 토픽에 메시지가 도착하면 바로 파티션에 전달되어 소비자가 즉시 소비 가능<br>* 파티션은 append-only이므로 메시지 시간을 조절하기 어렵고 애플리케이션 수준에서 구현해야 함 |
| 우선순위     | priority queue를 지원하여 우선 순위에 따라서 처리가 가능하다 | 변경 불가능한 시퀀스 큐로, 한 파티션 내에서는 시간 순서를 보장한다. 하지만 여러 파티션의 병렬로 처리할 때는 시간 순서 보장 못함 |
| 장점         | - 오래전에 개발되어 제품 성숙도가 크다<br>- 필요에 따라 동기/비동기식 가능<br>- 유연한 라우팅<br>- producer/consumer간의 보장되는 메시지 전달<br>- Manage UI 기본 제공<br>- 데이터 처리보단 관리적 측면이나 다양한 기능 구현을 원할 때 사용 | - 이벤트가 전달되어도 삭제하지 않고 스트림에 저장<br>- 고성능, 고가용성, 분산처리에 효과적<br>- producer 중심적 (많은 양의 데이터를 병렬 처리) |
| 단점         | - Kafka 보다 느림 | - 범용 메시징 시스템에서 제공되는 다양한 기능이 제공되지 않음 |

>  참고)  
> - 카프카는 append만 가능한 순차적인 자료구조인 log를 기본 아키텍처로 사용

# 사용 사례
### 카프카
#### 이벤트 스트림 재생
- Kafka는 수신된 데이터를 다시 분석해야 하는 애플리케이션에 적합하다.
  - 보존 기간 내에 스트리밍 데이터를 여러 번 처리할 수도 있고 로그 파일을 수집하여 분석할 수도 있다.
- 사용된 메시지는 삭제되기 때문에 RabbitMQ를 사용하여 로그를 집계하기는 더 어렵다.
  
#### 실시간 데이터 처리
- Kafka는 메시지를 스트리밍할 때 지연 시간이 매우 짧기 때문에 스트리밍 데이터를 실시간으로 분석하는 데 적합하다.
  - 예를 들어 Kafka를 분산 모니터링 서비스로 사용하여 온라인 트랜잭션 처리 알림을 실시간으로 생성할 수 있다.

### 래빗엠큐
#### 복잡한 라우팅 아키텍처
- RabbitMQ는 요구 사항이 모호하거나 라우팅 시나리오가 복잡한 클라이언트에게 유연성을 제공한다.
  - 예를 들어 바인딩과 익스체인지를 통해 서로 다른 애플리케이션으로 데이터를 라우팅하도록 RabbitMQ를 설정할 수 있다.

#### 효과적인 메시지 전달
- RabbitMQ는 푸시 모델을 적용한다.
- 즉, 생산자는 클라이언트 애플리케이션이 메시지를 사용했는지 여부를 알 수 있다.
- 데이터를 교환하고 분석할 때 특정 순서 및 전달 보장을 준수해야 하는 애플리케이션에 적합하다. 

#### 다양한한 언어 및 프로토콜 지원
- 개발자는 MQTT 및 STOMP와 같은 레거시 프로토콜과 호환되어야 하는 클라이언트 애플리케이션에 RabbitMQ를 사용한다.
- 또한 RabbitMQ는 Kafka에 비해 더 많은 프로그래밍 언어를 지원한다.

#### 참고 페이지
- https://aws.amazon.com/ko/compare/the-difference-between-rabbitmq-and-kafka/
- https://seungyong20.tistory.com/entry/Kafka%EC%99%80-RabbitMQ%EB%A5%BC-%EC%95%8C%EC%95%84%EB%B3%B4%EC%9E%90
- https://velog.io/@mdy0102/MQ-%EB%B9%84%EA%B5%90-Kafka-RabbitMQ-Redis

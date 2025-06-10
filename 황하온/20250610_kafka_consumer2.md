## offset commit 오프셋 커밋
컨슈머는 토픽의 특정 파티션으로부터 데이터를 가져가서 처리한 다음, 이 파티션의 어느 레코드까지 소비했는지 확인하기 위해 오프셋을 커밋한다.
- Consumer는 poll() 메소드를 이용하여 주기적으로 브로커의 토픽 파티션에서 메시지를 가져오고 메시지를 성공적으로 가져 왔으면 commit을 통해서 __consumer_offse 에 다음에 읽을 offset 위치를 기록한다.

### __consumer_offsets
![image](https://github.com/user-attachments/assets/738aea78-9b87-489d-8be6-fecd9a5f8e66)

커밋한 오프셋은 브로커의 __consumer_offsets 이라는 내부 토픽에 저장된다. __consumer_offsets 토픽은 컨슈머 그룹별로 관리된다.  
리밸런싱이 발생하는, 예를 들어 하나의 컨슈머가 특정 컨슈머 그룹으로 새롭게 접속하는 상황에서 새롭게 파티션을 할당받는 컨슈머는 __consumer_offsets에 있는 offset 정보를 기반으로 메시지를 가져오기 때문에 메시지 중복이나 손실이 발생하지 않을 수 있다.  

#### __consumer_offsets 저장
- 컨슈머 그룹의 컨슈머가 모두 종료되어도 offset 정보는 기본적으로 7일동안 __consumer_offsets에 저장된다. 
  - offsets.retention.minutes(default 10080, 7일, 버전 2 이상) 으로 설정할 수 있다.
- Topic이 삭제되고 재생성될 경우에 해당 offset 정보는 0으로 __consumer_offsets에 기록된다.
 
### auto.offset.reset
- auto.offset.reset 은 __consumer_offsets에 컨슈머 그룹이 해당 토픽의 파티션 offset 정보를 가지고 있지 않을 때, 컨슈머가 접속하면(예를 들어 consumer 배포, consumer rebalancing 발생시) 파티션의 처음 offset 부터(earliest) 가져올 것인지, 최신 메세지부터(latest) 가져올 것인지를 설정하는 파라미터이다.
???
 
#### __consumer_offsets 에 오프셋이 저장되어 있을 경우
- earliest, latest 모두 `마지막 오프셋 이후`의 메시지를 읽는다.
 
#### __consumer_offsets 에 오프셋이 저장되어 있지 않을 경우
- earliest는 0번 오프셋 부터 메시지를 읽는다.
- latest는 최신의 메시지를 읽는다
- none의 경우, exception 이 발생한다.

#### latest를 지양해야 하는 이유

- 파티션의 개수가 변경될 경우 컨슈머는 `metadata.max.age.ms 만큼 메타데이터 리프래시 기간` 이후 리밸런싱이 일어나 파티션 할당과정을 거치게 된다. 
  - 문제는 메타데이터 리프레시 기간(파티션 변경 여부를 알아차리는 시간)동안 새롭게 참가한 파티션에 데이터가 들어올 수 있다는 사실이다. 
  - 아래는 컨슈머의 auto.offset.reset이 latest일 경우 데이터가 일부 유실되는 모습을 표현한다.

![image](https://github.com/user-attachments/assets/657bc87c-3cb1-45f4-b81a-9f5e3b7ede25)
![image](https://github.com/user-attachments/assets/17faa94d-4037-4b93-9054-cc36ea552fc3)
- 파티션 추가

![image](https://github.com/user-attachments/assets/1d767177-9ae4-43d3-8e04-e5a3297e91a2)
- 메타데이터 리프레시 기간동안 메세지가 발행되었다.
  
![image](https://github.com/user-attachments/assets/e7e54c63-d9bb-4a10-ab80-26dc42a1c2d2)
- 리밸런싱이 일어난다.

![image](https://github.com/user-attachments/assets/2c739e92-9ca7-4787-87d5-8e25a53d6fb5)
- 기존 오프셋 정보가 없으므로 가장 최신의 메세지부터 읽어들여 메세지가 유실된다.
- 출처 : 데브원영님 블로그, 참고[2]

## Auto Commit
- Consumer의 파라미터로 enable.auto.commit=true(기본값)인 경우, auto.commit.interval.ms 값(기본 5초)마다 Consumer가 자동으로 Commit을 수행한다.
  - 메세지를 읽은 다음 브로커에 바로 commit 적용되는 것은 아니다. 
  - auto.commit.interval.ms 값만큼 지난 후 수행되는 `poll() 수행`에서 이전 poll()에서 가져온 마지막 메시지의 offset을 commit 한다.

#### auto.commit.interval.ms=5000 인 경우,
- 첫번째 poll() 다음 5초보다 작은 시간 이후 poll()이 수행된다면, offset을 commit하지 않는다.
- 5초가 지난 이후의 poll() 수행에서 이전 poll() 의 offset을 commit 한다.
- 컨슈머가 메세지를 소비하다가 커밋 이전에 비정상적으로 종료되면 현재까지 읽은 offset을 commit 하지 못해 메시지 중복 읽기가 발생할 수 있다.
  - __consumer_offsets 에 커밋된 offset 부터 메시지를 읽기 때문에 중복으로 읽히게 된다.

## Manual Commit
- Consumer Property의 enable.auto.commit = false 로 설정하고 명시적으로 동기/비동기 방식 오프셋 커밋을 적용할 수 있다.
  - Spring 을 활용하는 경우 Ackmode 까지 설정을 바꾸어야 수동 커밋으로 전환될 수 있다.

### 동기 오프셋 커밋
- Consumer 객체의 commitSync() 메소드를 사용한다.
- 메시지 배치를 poll()을 통해서 읽어오고 해당 메시지들의 마지막 offset을 브로커에 commit 적용한다.
- 브로커에 commit 적용이 성공적으로 될 때까지 블로킹이 되고, Commit 완료 후에 다시 메시지를 읽어온다.
- 브로커에 Commit 전달이 실패할 경우 다시 Commit 수행을 요청한다.
- Commit 재시도가 일정 횟수 이상 실패할 경우 CommitFailedException이 발생한다.
- 비동기 방식 대비 더 느린 수행 시간을 보인다..

### 비동기 오프셋 커밋
- Consumer 객체의 commitAsync() 메소드를 사용한다.
- 메시지 배치를 poll()을 통해서 읽어오고, 해당 메시지들의 마지막 offset을 브로커에 commit 요청한다. 그러나, 브로커에 commit 적용이 성공적으로 되었음을 기다리지 않고(블로킹 하지 않음) 계속 메시지를 읽어온다.
- Callback(OffsetCommitCallback) 을 통해 Exception 을 처리할 수 있다.
- 브로커에 Commit 적용이 실패해도 다시 Commit 시도하지 않는다.
  - Consumer 장애 또는 Rebalance 시 한번 읽은 메시지를 다시 중복해서 가져 올 수 있다.
- 동기 방식 대비 더 빠른 수행 시간을 갖는다.

## 멀티 스레드 컨슈머
파티션을 여러개로 운영하는 경우, 데이터를 병렬 처리하기 위해서 파티션 개수와 컨슈머 개수를 동일하게 맞추는 것이 가장 좋은 방법이다.   
이 때, 컨슈머를 멀티 프로세스 방식이나 멀티 스레드 방식을 선택해 개발할 수 있다.  
자바는 멀티 스레드를 지원하므로 멀티 컨슈머 스레드를 개발할 수 있다. 이 때, 멀티 스레드로 컨슈머를 운영하기 위해서 각 컨슈머 스레드 간 영향을 미치지 않도록 스레드 세이프 로직, 변수를 적용해야 한다.
- 두 가지 멀티 스레드 컨슈머 생성 전략을 확인할 수 있다.
  - 스프링에서 제공하는 컨슈머는 후자에 해당한다.

### 컨슈머 멀티 워크 스레드 전략
- 컨슈머 멀티 워크 스레드 전략은 컨슈머 스레드는 1개만 실행하고, 데이터 처리를 담당하는 워커 스레드를 여러개 실행하는 전략이다.
- ExecutorService 자바 라이브러리를 사용하면 레코드를 병렬처리하는 스레드를 효율적으로 생성하고 관리할 수 있다.

#### 컨슈머 멀티 워크 스레드 전략 문제점
1. 데이터 유실
  - 각 레코드의 데이터 처리가 끝난 것을 확인하지 않고, 다음 poll() 메서드를 호출하기 때문에 데이터 처리가 스레드에서 진행 중일지라도 다음 poll() 메서드를 호출해 커밋을 수행하여, 데이터의 유실이 발생할 수 있다.
    - 처리 중이던 작업이 실패하는 경우, 메세지 재소비 불가하여 유실될 수 있다.
 
2. 레코드 처리 역전
- 스레드의 생성은 순서대로 진행되지만 나중에 생성된 스레드의 레코드 처리시간이 더 짧을 경우, 이전 레코드가 다음 레코드보다 나중에 처리될 수 있다.

### 컨슈머 멀티 스레드 전략
컨슈머 멀티 스레드 전략은 파티션의 개수만큼 컨슈머 스레드를 늘려서 운영하는 전략이다.  
데이터 유실의 가능성이 작고 파티션 별로 컨슈머에 할당해 레코드를 처리하므로, 메시지 키를 갖는 경우 레코드 처리 역전이 발생하지 않는다.

#### 스프링에서 설정
- concurrency 옵션 값을 적절하게 설정한다.(보편적으로 파티션 크기와 동일하게 설정한다.)

## 특정 파티션만 할당
- Consumer에게 여러 개의 파티션이 있는 Topic에서 특정 파티션만 할당할 수 있다.
  - 배치 처리시 특정 key레벨의 파티션을 특정 Consumer에 할당하여 처리할 경우에 적용한다.
- 특정 파티션의 특정 오프셋부터 메시지를 읽을 수도 있다.

## 리밸런스 리스너
- offset 을 커밋하지 못하고 리밸런스가 발생할 수 있다. 이 때, 마지막 offset 커밋 이후 읽은 메시지는 리밸런스가 완료되고 중복해서 읽게된다.   
  - 이런 문제를 해결하기 위해 리밸런스가 발생하면 현재까지 처리한 메시지를 기준으로 커밋을 시도해야 한다.   
  - 리밸런스 발생을 감지하기 위해 카프카 라이브러리는 ConsumerRebalanceListener 인터페이스를 지원한다.

```java
public static void main(String[] args) {
	// ...
    configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    consumer = new KafkaConsumer<>(configs);
    consumer.subscribe(Arrays.asList(TOPIC_NAME), new RebalanceListener());
    while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
        for (ConsumerRecord<String, String> record : records) {
            currentOffsets.put(
            	new TopicPartition(record.topic(), record.partition()), 
                new offsetAndMetadata(record + 1, null)
            );
        }
    }
}

public class RebalanceListener implements ConsumerRebalanceListener {
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
    }

    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        consumer.commitSync(currentOffsets);
    }
}
```
- onPartitionsAssigned() : 리밸런스가 끝나고, 파티션이 할당 완료되면 호출된다.
- onPartitionsRevoked() : 리밸런스가 시작되기 직전에 호출된다. 가장 마지막 레코드를 기준으로 커밋을 실시한다.

#### 스프링에서 설정
- 리스너 컨테이너 팩토리 객체에 setConsumerRebalanceListener() 메서드를 호출해 리밸런스 리스너를 만든다.

## 스프링 카프카 컨슈머
### 메시지 리스너
스프링 카프카에서는 리스너 컨테이너를 사용해 컨슈머를 2개의 타입으로 래핑하였다.

- 레코드 리스너(MessageListener) : 1개의 레코드를 처리
- 배치 리스너(BatchMessageListener) : 한 번에 여러개의 레코드들을 처리

### AcksMode
카프카 클라이언트에서는 3가지로 나뉘지만 스프링 카프카에서는 커밋의 종류를 7가지로 세분화 하고 로직을 만들어 놓았다.
- AckMode의 기본값은 BATCH이고, 컨슈머의 enable.auto.commit 옵션은 false로 지정한한다.

#### AcksMode 종류
AcksMode | 설명
--- | ---
RECORD |	레코드 단위로 프로세싱 이후 커밋
BATCH	| poll() 메서드로 호출된 레코드가 모두 처리된 이후 커밋. 기본값.
TIME | 특정 시간 이후에 커밋. 시간 간격을 선언하는 AckTime 옵션을 설정해야 한다.
COUNT	| 특정 개수만큼 레코드가 처리된 이후에 커밋, 레코드 개수를 선언하는 AckCount 옵션을 설정해야 한다.
COUNT_TIME | TIME, COUNT 옵션 중 맞는 조건이 하나라도 나올 경우 커밋
MANUAL	| Acknowledgement.acknowledge() 메서드가 호출되면 다음번 poll() 때 커밋을 한다. 매번 acknowledge() 메서드를 호출하면 BATCH 옵션과 동일하게 작동한다.
MANUAL_IMMEDIATE |	Acknowledgement.acknowledge() 메서드를 호출한 즉시 커밋한다.


### 참고 자료
- https://mandykr.tistory.com/97
- https://blog.voidmainvoid.net/514
- https://leejaedoo.github.io/kafka-detail_2/

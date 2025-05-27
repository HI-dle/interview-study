# Redis Persistence
- Redis는 인메모리 데이터베이스 이므로 서버 장애 발생시 데이터 유실 가능성이 높다.
- 이러한 문제점을 해결하기 위해 Redis는 영속성을 보완하는 기능을 제공하고 있다.
  - 대표적으로 RDB, AOF 2가지 방식

# RDB(Redis Database)
- RDB 영속화는 특정 시점마다 스냅샷을 저장하는 방식으로 동작한다.
  - 특정 시점의 메모리에 있는 데이터 전체를 바이너리 파일로 저장한다. 
- AOF 파일보다 사이즈가 작다. 즉, 파일 로드에 걸리는 시간이 AOF 방식보다 단축된다는 장점이 있다.
- 다만 RDB가 어느 시점에 새롭게 저장되기 전에 서버 장애가 발생하면, 가장 최근에 저장된 RDB 이후의 데이터는 유실할 수 밖에 없다는 단점이 존재한다.

## RDB 설정 옵션
```conf
save 900 1   # 900초(15분) 동안 1번 이상 key 변경이 발생하면 저장 
save 300 10   # 300초(5분) 동안 10번 이상 key 변경이 발생하면 저장 
save 60 10000  # 60초(1분) 동안 10000번 이상 key 변경이 발생하면 저장 
```
- redis.conf 파일에 save 파라미터를 통해 설정이 가능하다.
  - save 파라미터는 복수로 등록이 가능하며, or 조건으로 동작하게 된다.
```
dbfilename dump.rdb
```
- filename 은 기본적으로 dump.rdb 이다.

```
stop-writes-on-bgsave-error : yes or no, default yes
```
- 이 파라미터 설정 값이 yes (default)인 경우, 레디스는 RDB 파일을 디스크에 저장하다 실패하면, 모든 쓰기 요청을 거부한다.
- 서비스를 계속하는 것이 더 중요하고 모니터링이 잘 되어 있는 경우 no 로 설정한다.
```
rdbcompression : yes or no, default yes
```
- rdb 파일의 압축 여부를 설정한다.
- 압축 알고리즘은 LZF 이다.
  - 압축률은 그다지 높지는 않다고
```
rdbchecksum : yes or no, default yes
```
- RDB 파일 끝에 CRC64 checksum 값을 기록한다. 
  - 데이터영속성 보장이 강화되나 10% 정도의 성능오버헤드 발생

## RDB 수행 방식
- 기본적으로는 BGSAVE 와 같이 동작하는 것으로 추정
  - https://stackoverflow.com/questions/31433596/does-redis-rdb-run-bgsave-or-save
- 레디스 서버에서 명령어를 주고 수동 실행하는 경우
  - 2가지 명령어가 제공되며 동작 방식은 아래와 같다.

### SAVE
- 메인 프로세스가 직접 rdb 스냅샷을 생성한다.
- 해당 동작이 끝날 때까지 클라이언트의 명령을 수행할 수 없다는 문제가 발생하므로 주의가 필요하다.
### BGSAVE
- 메인 프로세스를 fork() 한 자식 프로세스가 생성되어 백그라운드로 동작한다.
  - fork() 를 수행하므로 메모리 소요가 크다.
- 자식 프로세스는 새 rdb 스냅샷을 temp 파일로 작성한다.
- 쓰기 작업이 완료되면, 기존의 파일을 temp 파일로 교체한다.

# AOF(Append Only File)
- 입력/수정/삭제 명령 등 쓰기 명령이 실행될 때마다 기록한다. 조회 명령은 기록되지 않는다. 
  - rdb 에 비해 파일 크기가 크다.
- 서버가 재시작될 때, log에 기록된 write/update 연산을 재 실행하는 형태로 데이터를 복구하는 방식이다.
  - 모든 연산을 재수행하므로 부하가 클 수 있음
- rewrite 를 수행하여 파일 크기를 줄일 수 있다.
  - 중복된 쓰기 작업을 최종적 결과의 명령어로 대체하여 축약할 수 있다.
    - 예를 들어 INCR key 1000번은 SET key 1000 하나로 수정될 수 있다.

## AOF 설정 옵션
```
appendonly yes
```
- aof 기능을 활성화합니다.
 - redis 서버가 재시작 될 때, 이 값이 no이면 appendonly 파일이 있더라도 읽어들이지 않는다.

``` 
appendfilename "appendonly.aof" 
```
- AOF 파일명을 지정한다.
```
appendfsync always or everysec or no
```
- AOF에 기록하는 시점을 설정한다.
  - always : 명령 실행 시 마다 AOF에 기록한다. 데이터 유실의 염려는 없으나, 성능이 매우 떨어진다.
  - everysec : 1초마다 AOF에 기록합니다. 1초 사이 데이터 유실될 수 있으나, 성능에 거의 영향을 미치지 않으면서 데이터를 보존할 수 있어, 일반적으로 권장된다. default 값
  - no : AOF에 기록하는 시점을 OS가 정한다. 일반적으로 리눅스의 디스크 기록 간격은 30초입니다. 데이터가 유실될 수 있다.
```
auto-aof-rewrite-percentage 100 # 1. 
auto-aof-rewrite-min-size 64mb # 2.
```
- rewrite 관련 설정
  1. AOF 파일 사이즈가 100% 이상(시작 시점 aof 파일 기준) 커지면 rewrite, 시작 시 파일 크기가 0 이면, min-size 옵션 기준 실행
  2. AOF 파일 사이즈가 64mb 이하면 rewrite를 하지 않음
  -  BGREWRITEAOF 커멘드를 이용해 CLI 창에서 수동으로 AOF 파일 재작성 할 수 있다.

### REWRITE 동작 방식
- 자식 프로세스를 fork() 한다.
- 자식 프로세스는 기본 AOF 를 새 AOF temp 파일에 쓴다.
- 동시에, 부모 프로세스는 새로운 증분 AOF 파일(incremental AOF) 을 열어 이후 변경 사항들을 계속 기록한다.
  - 만약 리라이트(재작성)가 실패하더라도,
    - 이전의 **기존 base AOF 파일, 기존 increment 파일들(있다면) 그리고 지금 부모가 작성 중인 새로운 increment 파일**을 통해 전체 데이터를 복원할 수 있기 때문에 데이터 손실을 막을 수 있다.
- 자식 프로세스가 AOF 파일 작성을 마치면, 부모 프로세스는 시그널을 받고, **자식이 생성한 base 파일과, 자신이 열어놓은 increment 파일을 합쳐 임시 manifest 파일**을 생성하고 이를 디스크에 저장한다.
- 마지막 단계에서는 Redis가 manifest 파일을 원자적으로 교체함으로써, 이번 AOF 리라이트 결과가 실제로 적용된다.
- 이후 Redis는 **이전 base 파일과 사용되지 않는 increment 파일들을 정리(삭제)** 한다.

# RDB vs AOF 선택
- 우선 redis를 캐시로만 사용한다면 "굳이" 백업 기능은 필요 없다. 저장 공간 낭비가 될 수 있다.
- 그래도 백업은 필요하지만 어느 정도의 데이터 손실이 발생해도 괜찮은 경우, **RDB를 단독** 사용하는 것을 고려한다.
- 하지만, 장애 상황 직전까지 모든 데이터가 보장되어야 할 경우 AOF 사용(appendonly yes)하면 된다.
- Postgresql 만큼 강력한 영속성이 필요한 경우, RDB와 AOF 방식의 장단점을 상쇄하기 위해서 두가지 방식을 혼용해서 사용하는 것이 바람직하다.
  - 주기적으로 RDB(snapshot)으로 백업하고, 다음 snapshot까지의 저장을 AOF 방식으로 수행하는 식으로 혼용한다.

### 참고 페이지
- https://inpa.tistory.com/entry/REDIS-📚-데이터-영구-저장하는-방법-데이터의-영속성#rdb_snapshotting_방식
- http://redisgate.kr/redis/configuration/persistence.php#rdb_start
- https://escapefromcoding.tistory.com/707
- https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
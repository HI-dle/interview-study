# 기초 지식 보충

---
### ConcurrentHashMap은 왜 HashMap보다 안전한가?

- 멀티스레드 환경에서 HashMap은 동기화 처리가 되어 있지 않아 동시에 여러 스레드가 put 또는 resize 작업을 수행할 경우 무한 루프, 데이터 손실, 구조 붕괴와 같은 문제가 발생할 수 있다. 예시로 리사이징 도중 다른 스레드가 버킷에 접근하면서 해시 충돌로 인한 순환 참조가 생길 수 있다.

 - 반면 ConcurrentHashMap은 다음과 같은 구조적 개선을 통해 동기화 문제를 해결하고 있다.

   - Java 7까지는 Segment(세그먼트)라는 구조를 사용하여 맵을 여러 구역으로 나눈 후 Segment 단위로 락을 걸어 동시성 확보
   - Java 8부터는 Segment를 제거하고 배열의 각 Bucket(버킷)에 대해 CAS(Compare-And-Swap) 와 synchronized 블록을 혼합하여 성능과 안정성을 확보
   - 해시 충돌이 빈번한 경우에는 연결 리스트를 트리(TreeBin, red-black tree)로 전환하여 시간 복잡도를 개선 (O(n) → O(log n))
 - 락의 범위를 최소화하면서도 동시성과 일관성을 확보한 것이 핵심.

---

### synchronized 대신 ReentrantLock을 쓰기도 하나요?

 - synchronized는 Java 언어 차원에서 제공하는 기본적인 락이며 사용이 간단하고 JVM 수준에서 최적화도 많이 되어 있다.

 - 락 타임아웃 불가로 락 대기 중 인터럽트 불가, 락 공정성(Fairness) 설정 불가, 락 획득 여부 확인 불가로 무조건 대기라는 한계가 존재한다.

 - 이러한 제약을 해결하기 위해 java.util.concurrent.locks.ReentrantLock을 사용한다. 이 클래스는 명시적인 락 제어가 가능하고 다음과 같은 장점이 있다

   - tryLock()으로 락 즉시 획득 가능 여부 확인
   - lockInterruptibly()로 인터럽트 가능 대기
   - 공정성 설정 가능 (new ReentrantLock(true)로 FIFO 방식)
   - 조건 변수(Condition) 사용 가능 → wait/notify를 대체하면서 더 정교한 제어 가능
 - 세밀한 동시성 제어, 비동기 응답, 락 타임아웃 제어가 필요한 고성능 시스템에서 ReentrantLock이 선호된다.

---

### Java에서 메모리 누수가 발생할 수 있는 상황

 - Java는 가비지 컬렉터(GC)를 통해 메모리를 자동으로 관리하지만 참조가 남아 있는 객체는 GC 대상이 아니기 때문에 메모리 누수가 발생할 수 있다.

 1. Static 컬렉션에 객체를 계속 추가 후 제거하지 않음
    - ex) static List<User> cache에 계속 추가하면서 제거 로직이 없으면 메모리 고갈
 2. 이벤트 리스너나 콜백 등록 후 해제하지 않음
    - GUI나 서버에서 addListener()만 호출하고 removeListener()를 호출하지 않으면 참조가 계속 남아 있음
 3. ThreadLocal 사용 후 remove()를 호출하지 않음
    - 특히 ThreadPool과 함께 사용하면, Thread가 재사용되며 이전 요청의 값이 누수될 수 있음
 4. 내부 클래스가 외부 클래스를 암묵적으로 참조할 때
    - ex) new Runnable() { ... }에서 내부 익명 클래스가 외부 클래스의 참조를 계속 유지하면 GC 대상이 안 됨
 - 이런 참조들은 GC가 객체를 수거하지 못하게 만들어 점진적으로 힙을 채우고 OutOfMemoryError를 유발할 수 있다.

---

### final 키워드는 어떤 용도로 사용되며 객체에선 어떤 의미인가

 - final은 변경 불가능함(immutable)을 의미하며 3가지 용도로 사용된다

   - 변수에 사용 → 값을 한 번만 설정 가능
   - 메서드에 사용 → 오버라이딩 불가
   - 클래스에 사용 → 상속 불가
   - 객체에 final이 붙은 경우
````java
final User user = new User();
user = new User(); //  불가능
user.setName("Alice"); //  가능
````
- 참조는 변경 불가하지만 객체의 내부 상태는 변경 가능하다. 진정한 불변 객체를 만들려면 내부 필드도 모두 final로 선언하고 setter 없이 생성자만으로 값을 설정해야 한다.

---

### Java에서 HashCode와 equals는 왜 같이 오버라이드해야 하나

 - HashMap, HashSet과 같은 컬렉션에서 객체를 비교할 때 equals()와 hashCode()는 다음과 같은 방식으로 함께 사용된다

 1. hashCode()로 버킷 위치를 찾고
 2. 그 위치에서 equals()로 실제 객체 비교를 수행
 - 따라서 equals()가 같으면 반드시 hashCode()도 같아야 함 그렇지 않으면 같은 객체라도 서로 다른 버킷에 저장되어 중복 저장되거나 검색 실패 가능

````java
@Override
public boolean equals(Object o) { ... }

@Override
public int hashCode() { ... }


````

 - 반드시 둘을 같이 오버라이드해야 Java 컬렉션이 의도대로 동작한다.

---


### ThreadPool을 왜 써야 하나

 - 스레드는 Java에서 병렬 처리를 위해 필수적인 요소지만 스레드를 무제한으로 생성하는 것은 매우 비효율적이며 위험하다.

 - 스레드 생성 시 다음과 같은 비용과 리스크가 존재한다.

   - 스택 메모리 할당: 기본적으로 스레드 하나당 수백 KB~1MB의 스택이 필요함
   - 스레드 생성 및 종료 비용: JVM이 스레드를 OS 수준에서 관리하므로 생성/정리 시 오버헤드 발생
   - CPU 컨텍스트 스위칭 비용: 스레드가 많아질수록 전환 비용 증가로 오히려 성능 저하
 - 이러한 문제를 해결하기 위해 ThreadPool(스레드 풀)을 사용한다.

 - ThreadPool의 주요 목적과 장점

 - 스레드 재사용 (Thread Reuse)
   - 한 번 생성된 스레드를 작업에 재사용으로  불필요한 생성·제거 반복 방지
 - 리소스 제어 (Resource Control)
   - 동시에 실행 가능한 스레드 수를 제한으로 OutOfMemoryError, CPU 과부하 방지
 - 성능 예측 가능성
   - 실행 환경에서의 스레드 수가 고정되므로 TPS(초당 처리량) 및 시스템 반응 시간 예측이 가능
 - 작업 큐 기반 처리
   - 작업이 몰릴 경우 대기 큐에 적재하여 스레드가 유휴 상태일 때 처리를 통한 부하 분산
 - 운영 안정성
   - 운영 환경에서는 서버 리소스가 유한하기 때문에 풀링 구조를 통해 예측 가능하고 안정적인 서비스 제공이 중요

---

### Volatile 키워드에 대해

 - 가시성 보장
 - 자바에서 스레드는 성능을 높이기 위해 주 메모리 (힙과 같이 모든 스레드가 공유하는 메모리)에 대해 로컬 메모리(CPU 레지스터 또는 캐시)에 복사해서 작업한다. 이후 작업이 끝나면 주 메모리에 기록한다. 따라서 다른 스레드가 주 메모리에 기록하기 전에는 최신값을 보지 못한다. volatile으로 선언하면, 모든 쓰기와 읽기가 주 메모리에서 이루어진다.
 - 자바에서는 성능 최적화를 위해 프로그램의 최종 결과에 영향을 주지 않는 범위에서 명령어를 재정렬한다. 이는 명령어 재정렬이 가시성 보장을 하지 않게 한다.
   - 객체 생성 과정의 3단계 (이 중에 2,3 단계가 재정렬될 수 있어서, 다른 스레드에서 초기화 되지 않은 객체의 변수에 접근할 수 있음)
     - 1단계: 메모리 할당
     - 2단계: 생성자 호출로 객체 초기화
     - 3단계: 객체 참조를 변수에 할당 (instance에 연결)
 - 꼬리질문: 그럼 왜 2,3 단계를 변경하는 것이 효율적일까요?
   - CPU 병렬 처리 최적화: 참조 연결을 먼저 수행하여 다음 작업의 대기 시간을 줄임.
     - CPU는 객체 초기화가 참조처리보다 오래 걸린다고 판단할 수 있음. 따라서 객체 초기화를 나중에 하는 걸로 미루는 것임.
   - 캐시 효율성 증가: 객체 참조를 먼저 연결하여 캐시 적중률을 높임.
 - 꼬리질문: 그럼 동시성 문제에 volatile 만 있으면 해결될까요?
   - 아닙니다. 동시성 문제는 가시성, 원자성, 순서 재배치를 모두 포함한다. 동시에 메인 메모리에 쓰는 경우가 있으면 동시성에 문제가 있습니다. 그래서 이와 더불어 synchronzied 블록이나 java.util.concurrent.atomic 패키지의 클래스들을 사용해 동시성을 보장해야 한다.

---

### 직렬화에서 SerialVersionUID를 선언해야 하는 이유는?

 - Serializable을 상속하는 클래스의 경우 버저닝을 위해 serialVersionUID 변수를 사용한다. 이때 명시적으로 지정하지 않으면 컴파일러가 계산한 값을 부여하는데, 컴파일러에 따라 할당되는 값이 다를 수 있어 동일 버전임에도 다른 버전으로 취급할 수 있다. 값이 따르면 역직렬화 시 InvalidClassException 이 발생하여 이슈가 발생한다.

````java
package org.example;

import java.io.*;
import java.util.Objects;

public class SerializeUtil {

    public static final String path = "serial.ser";

    public static void write(Sparta sparta) {
        try (
                FileOutputStream fo = new FileOutputStream(path);
                ObjectOutputStream out = new ObjectOutputStream(fo)
                ) {

            out.writeObject(sparta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Sparta read() {
        try (FileInputStream fis = new FileInputStream(path);
        ObjectInputStream in = new ObjectInputStream(fis)) {

            Sparta sparta = (Sparta) in.readObject();
            return sparta;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        Sparta sparta = new Sparta();
        sparta.setName("test");

//        SerializeUtil.write(sparta);

        Sparta readSparta = SerializeUtil.read();
        System.out.println(readSparta);
    }

    public static class Sparta implements Serializable {

        private static final long serialVersionUID = 2L; // java.io.InvalidClassException: org.example.SerializeUtil$Sparta; local class incompatible: stream classdesc serialVersionUID = 1, local class serialVersionUID = 2

        String name;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Sparta{" +
                    "name='" + name + '\\'' +
                    '}';
        }
    }
}
````

 - 꼬리질문: 직렬화 관련하여 Spring Security 버전 업데이트 시 고려해야 할 사항
   - spring security SecurityContextImpl 클래스에서는 스프링 시큐리티 버전값으로 사용해서 버전이 변경될 때마다 고려해야 함
   - 그렇지 않으면 세션에 저장되어이 있는 값이 역직렬화가 되지 않는 에러를 겪음
 - 꼬리 질문: 호환성 기준
   - 같은 클래스 버전으로 인식하는 경우
     - 필드 추가
     - 클래스 제거
     - serializable 추가
     - 멤버변수의 접근 지정자 변경
     - static 필드 → non static 필드 및 transient → non-transient로 변경 (필드 추가와 동일)
오늘은 두가지 내용을 준비했습니다.
첫번째로는 `Integer Caching`이고, 두번째는 `자바에서 기본형 타입과 참조형 타입의 결정적 차이` 라는 주제입니다.
# 1. Integer Caching

```java
public class Main {
  public static void main(String[] args) {
    Integer a = 128;
    Integer b = 128;
    
    if (a == b) {
      System.out.println("YES!");
    } else {
      System.out.println("NO!");
    }
  }
}
```

위의 결과는 어떻게 될까요?

정답은 `NO!` 입니다. 왜냐하면, `Integer`는 래퍼(참조) 타입이기 때문입니다.

사실 `Integer a = 128`은 자바 내부적으로 `Integer a = Integer.valueOf(128)`로 변환돼서 동작됩니다.

즉, `128`이라는 primitive type을 `Integer`라는 참조형 타입으로 자동으로 변환되어 저장되는, `autoboxing`이 일어납니다.

위 `Integer` 타입 `a`와 `b`는 아래 그림과 같이 메모리상에 존재하게 되는데요.

간단히 설명드려보면, `Integer a, b`는 객체이기 때문에 `heap` 영역에 저장됩니다. 또한 `stack` 영역에는 해당 객체들의 참조값을 저장하게 됩니다.

![integer-a-b.png](images%2Finteger-a-b.png)

따라서, 주소 값이 다르기 때문에 `NO!`가 출력된 것입니다.

그렇다면 아래와 같은 코드는 어떻게 될까요?
```java
public class Main {
  public static void main(String[] args) {
    Integer a = 127; // 127로 바꿔서 다시 실행해본다면?
    Integer b = 127;
    
    if (a == b) {
      System.out.println("YES!");
    } else {
      System.out.println("NO!");
    }
  }
}
```

결과는 `YES!` 입니다. 왜냐하면 자바에서 `Integer` 값들을 관리할 때, `-128 ~ 127` 범위는 캐싱을 해두기 때문입니다.

따라서 간단하게 그림으로 보면, 아래와 같이 됩니다.

![a==b.png](images%2Fa%3D%3Db.png)

> 그래도 개발할 때는 `Integer` 값을 비교할 때 equals를 사용해야합니다.

---

# 2. 자바에서 기본형 타입과 참조형 타입의 결정적 차이
자바의 기본형 타입으로는 정수형, 실수형, 문자형, 논리형이 존재합니다. 그리고 이 타입들을 제외한 나머지가 래퍼런스 타입입니다.

그럼 기본형과 참조형의 동작 방식에 있어서 결정적인 차이는 무엇일까요? 그것은 `메모리를 어떻게 사용하느냐` 에 있습니다.

```java
public class PrimitiveVsReference {
  public static void main(String[] args) {
    int a = 1;
    Integer b = 1;

    System.out.println(a);
    System.out.println(b);
  }
}
```

위와 같은 코드는 메모리상에서 아래 그림과 같이 저장됩니다.

![pvsr.png](images%2Fpvsr.png)

그래서 사실 기본형이 참조형보다 연산이 더 빠릅니다. 참조형 같은 경우 값에 접근하기 위해서는 주소 값을 통해서 `한 번 더 메모리에 접근하는 과정`이 필요합니다.

> 모든 언어마다 설계 철학이 존재합니다. 자바의 경우는 위 처럼, 간단한 로직은 기본형으로 구현 시 좀 더 빠르게 구현할 수 있도록 설계되었습니다.
> 
> 반면에 파이썬은 '모든 것은 객체다'라는 철학을 갖고 있다고 합니다. 따라서 기본형 타입이라는 개념이 없다고 합니다.

---

# 3. 자바에서 래퍼 클래스는 언제 써야 할까?

래퍼 클래스(wrapper class)란 `기본형 타입을 포장한 클래스`로 기본형 타입의 데이터를 객체로 쓸 수 있게 해주는 클래스입니다.

> 위에서 언급했 듯, 기본형 타입이 연산이 더 빠릅니다. 그래서 래퍼 타입이 꼭 필요한 경우가 아니라면, 기본형 타입을 적극적으로 사용하는 것이 좋습니다.

### 래퍼 클래스를 사용해야할 때
- 제네릭
- null 처리가 필요한 경우
- 유틸 메서드(`Integer.parseInt("123")`)


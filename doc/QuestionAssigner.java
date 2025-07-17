import java.time.LocalDateTime;
import java.util.*;

public class QuestionAssigner {

  private static final String 박지은 = "박지은";
  private static final String 황하온 = "황하온";
  private static final String 한지훈 = "한지훈";
  private static final String 강혜주 = "강혜주";
  private static final String 최진영 = "최진영";
  private static final String TYPE = "TYPE";

  private static final String messaging = "messaging";
  private static final String network = "network";
  private static final String os = "os";
  private static final String architecture = "architecture";
  private static final String spring = "spring";
  private static final String db = "db";
  private static final String java = "java";

  public static void main(String[] args) {
    StringBuilder sb = new StringBuilder();

    List<String> people = new ArrayList<>();
    people.add(박지은);
    people.add(황하온);
    people.add(한지훈);
    people.add(강혜주);
    people.add(최진영);
    people.add("남정길");

    Map<String, List<Question>> assignment = assignQuestions(people);

    int idx = 1;
    for (Map.Entry<String, List<Question>> entry : assignment.entrySet()) {
      System.out.println(idx + ". " + entry.getKey() + "에게 배정된 질문:");
      for (Question q : entry.getValue()) {
        System.out.println(" - [" + q.getCategory() + "] " + q.getQuestion() + " (날짜: " + q.getDate() + ")");
        sb.append("questions.add(new Question(\"\", week9999, ").append(q.getCategory()).append(", ").append(entry.getKey()).append("));");
        sb.append('\n');
      }
      System.out.println();
      idx++;
    }

    System.out.println(sb);
  }

  private static List<Question> getQuestions() {
    List<Question> questions = new ArrayList<>();

    LocalDateTime week1 = LocalDateTime.of(2025, 6, 13, 0, 0);
    questions.add(new Question("TCP의 연결 및 해제 방식에 대해 설명해주세요", week1, network, 강혜주));
    questions.add(new Question("TCP / UDP 차이에 대해 설명해주세요", week1, network, 강혜주));
    questions.add(new Question("Spring Bean Scope 란 무엇인가요?", week1, spring, 박지은));
    questions.add(new Question("Spring Proxy 에 대해 설명해주세요.", week1, spring, 박지은));
    questions.add(new Question("RabbitMQ와 카프카의 장단점을 각각 설명해주세요", week1, messaging, 최진영));
    questions.add(new Question("메시지 큐는 무엇이고, 왜 사용하나요?", week1, messaging, 최진영));
    questions.add(new Question("모놀리식와 MSA 차이에 대해 설명해주세요.", week1, architecture, 한지훈));
    questions.add(new Question("MSA 아키택처로 구성하게 될 때 모놀리식 대비 단점을 설명해주시고 단점을 감안하며 MSA 구성하는 이유에 대해 설명해주세요.", week1, architecture, 한지훈));
    questions.add(new Question("세마포어 semaphore 에 대해 설명해보세요.", week1, os, 황하온));
    questions.add(new Question("인터럽트 interrupt 에 대해 설명해주세요.", week1, os, 황하온));

    LocalDateTime week2 = LocalDateTime.of(2025, 6, 20, 0, 0);
    questions.add(new Question("www.google.com에 접속할 때의 과정에 대해 설명해주세요", week2, network, 강혜주));
    questions.add(new Question("HTTP 프로토콜에 대해 아는대로 설명해주세요.", week2, network, 강혜주));
    questions.add(new Question("프로세스 구조에 대해 설명해주세요.", week2, os, 박지은));
    questions.add(new Question("컨텍스트 스위칭에 대해 설명해주세요.", week2, os, 박지은));
    questions.add(new Question("Index에 대해 아는대로 설명해주세요.", week2, db, 최진영));
    questions.add(new Question("트랜잭션의 특성에 대해 설명해주세요.", week2, db, 최진영));
    questions.add(new Question("분산 아키텍처 환경에서 메시징으로 어떻게 데이터 일관성을 보장할 수 있을까요?", week2, messaging, 한지훈));
    questions.add(new Question("메시지 손실 방지를 위해 어떤 방법을 쓸 수 있나요?", week2, messaging, 한지훈));
    questions.add(new Question("스프링 Event 기능에 대해서 아는대로 설명해주세요.", week2, spring, 황하온));
    questions.add(new Question("JPA N+1 문제에 대해서 아는대로 설명해주세요.", week2, spring, 황하온));

    LocalDateTime week3 = LocalDateTime.of(2025, 6, 27, 0, 0);
    questions.add(new Question("카프카의 컨슈머 그룹에 대해 설명해주세요.", week3, messaging, 강혜주));
    questions.add(new Question("정규화에 대해 설명해주세요", week3, db, 강혜주));
    questions.add(new Question("Spring Boot란 무엇인가요? 장점은 무엇인가요?", week3, spring, 박지은));
    questions.add(new Question("프로세스간에는 어떤 기술을 사용해서 통신하는지, 왜 해당 기술을 사용해서 통신해야하는지를 쓰레드와 비교해서 설명해주세요.", week3, os, 박지은));
    questions.add(new Question("final 키워드의 이점과 컴파일 과정에서 어떻게 다른 지 설명해주세요", week3, java, 최진영));
    questions.add(new Question("equals(), hashCode()에 대해 설명해주세요", week3, java, 최진영));
    questions.add(new Question("DDD에 대해서 설명해주세요.", week3, architecture, 한지훈));
    questions.add(new Question("Java에서 메모리 누수가 발생할 수 있는 상황을 설명해주세요.", week3, java, 한지훈));
    questions.add(new Question("https 프로토콜의 동작방식은 어떻게 되나요?", week3, network, 황하온));
    questions.add(new Question("데이터베이스의 락에 대해서 설명해주세요.", week3, db, 황하온));

    LocalDateTime week4 = LocalDateTime.of(2025, 7, 4, 0, 0);
    questions.add(new Question("단일 장애 지점(SPOF)이란 무엇인가요?", week4, architecture, 강혜주));
    questions.add(new Question("Call By Value와 Call By Reference에 대해서 설명해주세요.", week4, java, 강혜주));
    questions.add(new Question("가상 메모리와 페이징 시스템에 대해 설명해주세요", week4, os, 박지은));
    questions.add(new Question("RDB와 NoSQL의 차이에 대해 설명해 주세요.", week4, db, 박지은));
    questions.add(new Question("IoC와 DI에 대해 설명해주세요.", week4, spring, 최진영));
    questions.add(new Question("String, StringBuilder, StringBuffer의 차이점을 설명해주세요.", week4, java, 최진영));
    questions.add(new Question("트랜잭션 전파 속성을 설명해주세요.", week4, spring, 한지훈));
    questions.add(new Question("자바 예외 종류와 예외 처리 방법에 대해 설명해주세요.", week4, java, 한지훈));
    questions.add(new Question("카프카 ISR 그룹에 대해 설명해주세요.", week4, messaging, 황하온));
    questions.add(new Question("http 1.1과 2 버전의 차이에 대해서 설명해주세요.", week4, network, 황하온));

    LocalDateTime week5 = LocalDateTime.of(2025, 7, 11, 0, 0);
    questions.add(new Question("", week5, architecture, 황하온));
    questions.add(new Question("", week5, db, 황하온));
    questions.add(new Question("", week5, db, 강혜주));
    questions.add(new Question("", week5, java, 강혜주));
    questions.add(new Question("", week5, spring, 최진영));
    questions.add(new Question("", week5, java, 최진영));
    questions.add(new Question("", week5, messaging, 박지은));
    questions.add(new Question("", week5, java, 박지은));
    questions.add(new Question("", week5, java, 한지훈));
    questions.add(new Question("", week5, os, 한지훈));
    return questions;
  }

  private static List<Question> getQuestionTypes() {
    List<Question> questions = new ArrayList<>();

    LocalDateTime week1 = LocalDateTime.of(9999, 12, 31, 0, 0);
    questions.add(new Question("", week1, network, TYPE));

    questions.add(new Question("", week1, spring, TYPE));
    questions.add(new Question("", week1, spring, TYPE));
    questions.add(new Question("", week1, spring, TYPE));

    questions.add(new Question("", week1, messaging, TYPE));

    questions.add(new Question("", week1, architecture, TYPE));

    questions.add(new Question("", week1, os, TYPE));

    questions.add(new Question("", week1, java, TYPE));
    questions.add(new Question("", week1, java, TYPE));
    questions.add(new Question("", week1, java, TYPE));

    questions.add(new Question("", week1, db, TYPE));
    questions.add(new Question("", week1, db, TYPE));

    return questions;
  }

  private static Map<String, List<Question>> assignQuestions(List<String> people) {
//    List<Question> questions = getQuestions();
    List<Question> questions = getQuestionTypes();

    if (people.size() * 2 > questions.size()) {
      throw new IllegalArgumentException("질문 개수가 부족합니다. (사람 수 * 2 <= 질문 수)");
    }

    List<Question> shuffled = new ArrayList<>(questions);
    Collections.shuffle(shuffled);

    Map<String, List<Question>> result = new LinkedHashMap<>();
    Iterator<Question> iterator = shuffled.iterator();

    Collections.shuffle(people);
    for (String person : people) {
      List<Question> assigned = new ArrayList<>();
      assigned.add(iterator.next());
      assigned.add(iterator.next());
      result.put(person, assigned);
    }

    return result;
  }

  static class Question {
    private String question;
    private LocalDateTime date;
    private String category;
    private String preparer;

    public Question(String question, LocalDateTime date, String category, String preparer) {
      this.question = question;
      this.date = date;
      this.category = category;
      this.preparer = preparer;
    }

    public String getQuestion() {
      return question;
    }

    public LocalDateTime getDate() {
      return date;
    }

    public String getCategory() {
      return category;
    }

    public String getPreparer() {
      return preparer;
    }
  }
}


# 🏦 FinMate

![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/mango606/finmate-backend?utm_source=oss&utm_medium=github&utm_campaign=mango606%2Ffinmate-backend&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

## 📋 프로젝트 정보

### 🛠️ 기술 스택

- **Language**: Java 17
- **Framework**: Spring Legacy 5.3.37
- **Build Tool**: Gradle 8.x
- **Database**: MySQL 8.0
- **ORM**: MyBatis 3.5.13
- **Security**: Spring Security 5.8.13 + JWT
- **Server**: Gretty (Embedded Tomcat 9)
- **API Documentation**: Swagger 2.9.2

## 📁 프로젝트 구조

```
finmate-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/finmate/
│   │   │       ├── common/          # 공통 유틸리티
│   │   │       ├── config/          # 설정 클래스
│   │   │       ├── member/          # 회원 관리
│   │   │       │   ├── controller/
│   │   │       │   ├── service/
│   │   │       │   ├── mapper/
│   │   │       │   ├── dto/
│   │   │       │   ├── domain/
│   │   │       │   └── exception/
│   │   │       └── security/        # 보안 관련
│   │   │           ├── config/
│   │   │           ├── filter/
│   │   │           ├── handler/
│   │   │           ├── util/
│   │   │           └── account/
│   │   ├── resources/
│   │   │   ├── com/example/finmate/ # MyBatis 매퍼 XML
│   │   │   ├── application.properties
│   │   │   ├── mybatis-config.xml
│   │   │   └── log4j2.xml
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   ├── web.xml
│   │       │   ├── applicationContext.xml
│   │       │   ├── servlet-context.xml
│   │       │   └── security-context.xml
│   │       ├── resources/
│   │       └── index.html
│   └── test/
│       └── java/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── .gitignore
└── README.md
```

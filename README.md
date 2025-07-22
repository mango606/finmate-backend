# 🏦 FinMate - 제테크 금융 서비스

<div align="center">

**개인 맞춤형 제테크 솔루션을 제공하는 금융 서비스**

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat&logo=openjdk&labelColor=171717)](https://openjdk.org/)
[![Spring](https://img.shields.io/badge/Spring-5.3.37-green?style=flat&logo=spring&labelColor=171717)](https://spring.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat&logo=mysql&labelColor=171717)](https://www.mysql.com/)
[![JWT](https://img.shields.io/badge/JWT-Security-red?style=flat&logo=jsonwebtokens&labelColor=171717)](https://jwt.io/)

![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/mango606/finmate-backend?utm_source=oss&utm_medium=github&utm_campaign=mango606%2Ffinmate-backend&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)

[🚀 빠른 시작](#-빠른-시작) • [📋 주요 기능](#-주요-기능) • [🛠️ 기술 스택](#️-기술-스택) • [📖 API 문서](#-api-문서) • [🤝 기여하기](https://github.com/mango606/finmate-backend/fork)

</div>

---

## 📊 프로젝트 개요

**FinMate**는 개인의 금융 목표 달성을 지원하는 제테크 플랫폼입니다. 사용자의 수입, 지출, 투자 성향을 분석하여 맞춤형 금융 솔루션을 제공하고, 체계적인 자산 관리를 도와줍니다.

### 🎯 서비스 목표
- **개인 맞춤형 금융 목표 설정** 및 체계적 관리
- **투자 성향 분석**을 통한 기본 포트폴리오 추천
- **수동 입력 기반 자산 현황** 모니터링 및 분석

---

## 🚀 빠른 시작

### 📋 사전 요구사항

```bash
# 필수 요구사항
- Java 17 이상
- Docker & Docker Compose
- Git

# 개발 환경 (선택사항)
- IntelliJ IDEA 또는 Eclipse
- MySQL Workbench
- Postman (API 테스트)
```

### ⚡ 1분 만에 실행하기

```bash
# 1. 프로젝트 클론
git clone https://github.com/mango606/finmate-backend.git
cd finmate-backend

# 2. 환경 설정 및 실행 (Windows)
# 또는 Linux/macOS에서는 sleep 60 사용
docker-compose down -v && docker volume prune -f && docker-compose up -d && timeout /t 60 && docker exec finmate-mysql mysql -u finmate -p1234 -e "SELECT 'OK' as status;" && ./gradlew clean build && ./gradlew runServer
```

### 🐳 상세 실행 단계

#### 1단계: 데이터베이스 환경 구성

```bash
# 기존 컨테이너 및 볼륨 정리
docker-compose down -v
docker volume prune -f

# MySQL 컨테이너 시작 (데이터베이스 + phpMyAdmin)
docker-compose up -d

# 데이터베이스 초기화 대기 (60초)
timeout /t 60  # Windows
# sleep 60     # Linux/macOS
```

#### 2단계: 데이터베이스 연결 확인

```bash
# MySQL 연결 테스트
docker exec finmate-mysql mysql -u finmate -p1234 -e "SELECT 'OK' as status;"

# 결과: 'OK' 출력되면 정상
# +--------+
# | status |
# +--------+
# | OK     |
# +--------+
```

#### 3단계: 애플리케이션 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew clean build

# 서버 실행
./gradlew runServer
```

### 🎉 실행 완료!

| 서비스 | URL                              | 설명 |
|--------|----------------------------------|------|
| **메인 서버** | http://localhost:8080            | FinMate 백엔드 API 서버 |
| **API 문서** | http://localhost:8080/swagger-ui/index.html           | Swagger UI API 문서 |
| **데이터베이스 관리** | http://localhost:8081            | phpMyAdmin (root/1234) |
| **헬스 체크** | http://localhost:8080/api/health | 서버 상태 확인 |

---

## 📋 주요 기능

### 🔐 회원 관리 & 보안
- **회원가입/로그인** - JWT 기반 보안 인증
- **이메일 인증** - 계정 활성화 및 보안 강화
- **비밀번호 재설정** - 안전한 비밀번호 복구
- **계정 보안 관리** - 로그인 실패 횟수 제한 및 계정 잠금

### 💰 금융 프로필 관리
```json
{
  "monthlyIncome": 5000000,
  "monthlyExpense": 3000000,
  "riskProfile": "MODERATE",
  "investmentGoal": "RETIREMENT",
  "investmentPeriod": 240
}
```

### 🎯 금융 목표 설정
- **목표 유형**: 적금, 투자, 내집마련, 은퇴준비
- **진행률 추적**: 자동 달성률 계산
- **목표 관리**: 생성, 수정, 삭제 및 상태 관리

### 📊 투자 포트폴리오 추천
- **위험 성향 분석**: 보수적/중립적/공격적 분류
- **룰 기반 포트폴리오 추천**: 나이와 성향에 따른 기본 자산 배분
- **월 투자 금액 계산**: 여유 자금 기반 투자액 추천

### 📈 거래 내역 관리
- **수입/지출 입력**: 사용자 직접 입력
- **카테고리 분류**: 식비, 교통비, 주거비 등
- **월별/연별 통계**: 기본 집계 및 차트

### 🎓 금융 교육 콘텐츠
- **텍스트 기반 가이드**: 제테크 기초 지식
- **간단한 퀴즈**: O/X 및 선택형 문제
- **외부 링크 큐레이션**: 유용한 금융 정보 모음
- **계산기 도구**: 복리 계산기, 목표 달성 시뮬레이터

---

## 🛠️ 기술 스택

### Backend
```yaml
Language: Java 17
Framework: Spring Legacy 5.3.37
Build Tool: Gradle 8.x
ORM: MyBatis 3.5.13
Security: Spring Security 5.8.13 + JWT
Server: Embedded Jetty 9.4.x
```

### Database
```yaml
RDBMS: MySQL 8.0
Connection Pool: HikariCP
Container: Docker Compose
Management: phpMyAdmin
```

### Development & DevOps
```yaml
Version Control: Git + GitHub
Documentation: Swagger 2.9.2
Testing: JUnit 5 + Mockito
Logging: SLF4J + Logback
Code Quality: GitHub Actions CI/CD
```

---

## 📁 프로젝트 구조

```
finmate-backend/
├── 🐳 docker/                     # Docker 설정
│   └── mysql/
│       ├── conf.d/                # MySQL 설정
│       └── init/                  # 초기 데이터베이스 스키마
├── 🏗️ src/main/java/com/example/finmate/
│   ├── 👤 member/                 # 회원 관리
│   │   ├── controller/
│   │   ├── service/
│   │   ├── mapper/
│   │   └── dto/
│   ├── 🔐 auth/                   # 인증/보안
│   │   ├── controller/
│   │   ├── service/
│   │   └── domain/
│   ├── 💰 financial/              # 금융 서비스
│   │   ├── controller/
│   │   ├── service/
│   │   └── mapper/
│   ├── 🔧 common/                 # 공통 기능
│   │   ├── util/
│   │   ├── exception/
│   │   └── dto/
│   ├── 🛡️ security/               # 보안 설정
│   └── ⚙️ config/                 # 애플리케이션 설정
├── 🧪 src/test/                   # 테스트 코드
├── 📋 src/main/resources/         # 설정 파일
└── 🌐 src/main/webapp/            # 웹 리소스
```

---

## 📖 API 문서

### 🔗 주요 엔드포인트

| 카테고리 | Method | Endpoint | 설명 | 상태 |
|----------|--------|----------|------|------|
| **인증** | `POST` | `/api/auth/login` | 로그인 | ✅ |
| **인증** | `POST` | `/api/auth/refresh` | 토큰 갱신 | ✅ |
| **인증** | `POST` | `/api/auth/logout` | 로그아웃 | ✅ |
| **회원** | `POST` | `/api/member/join` | 회원가입 | ✅ |
| **회원** | `GET` | `/api/member/info` | 회원정보 조회 | ✅ |
| **금융** | `GET` | `/api/financial/profile` | 금융 프로필 조회 | ✅ |
| **금융** | `POST` | `/api/financial/goals` | 금융 목표 등록 | ✅ |
| **금융** | `GET` | `/api/financial/dashboard` | 대시보드 조회 | ✅ |

### 📚 상세 API 문서

실행 후 [Swagger UI](http://localhost:8080/swagger-ui/index.html)에서 전체 API 명세를 확인할 수 있습니다.

---

## 🗄️ 데이터베이스

### 📋 주요 테이블

| 테이블명 | 설명 | 주요 컬럼 |
|----------|------|-----------|
| `tbl_member` | 회원 정보 | user_id, user_name, user_email |
| `tbl_member_auth` | 회원 권한 | user_id, auth (ROLE_USER, ROLE_ADMIN) |
| `tbl_financial_profile` | 금융 프로필 | monthly_income, risk_profile |
| `tbl_financial_goal` | 금융 목표 | goal_name, target_amount, status |
| `tbl_financial_transaction` | 거래 내역 | transaction_type, amount, category |
| `tbl_education_content` | 교육 콘텐츠 | title, content_type, content |

### 🔧 데이터베이스 관리

```bash
# phpMyAdmin 접속
open http://localhost:8081
# 로그인: root / 1234

# 직접 MySQL 접속
docker exec -it finmate-mysql mysql -u finmate -p1234

# 데이터베이스 백업
docker exec finmate-mysql mysqldump -u root -p1234 finmate_db > backup.sql

# 데이터베이스 복원
docker exec -i finmate-mysql mysql -u root -p1234 finmate_db < backup.sql
```

---

## 🧪 테스트

### 🏃‍♂️ 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests MemberControllerTest

# 테스트 리포트 생성
./gradlew test jacocoTestReport
```

### 📊 테스트 커버리지

| 모듈 | 커버리지 | 상태 |
|------|----------|------|
| Controller Layer | 85%+ | ✅ |
| Service Layer | 90%+ | ✅ |
| Utility Classes | 95%+ | ✅ |

### 🔍 주요 테스트 시나리오

- **회원가입/로그인** 플로우 테스트
- **JWT 토큰** 발급 및 검증
- **API 응답 형식** 표준화 확인
- **예외 처리** 시나리오 검증

---

## 🚀 개발 로드맵

### ✅ Phase 1: 구현
- 회원 관리 및 JWT 인증 시스템
- 기본 금융 프로필 및 목표 관리
- 룰 기반 포트폴리오 추천
- 기본 교육 콘텐츠 관리

### 🔄 Phase 2: 개발 중
- 거래 내역 수동 입력 시스템
- 월별/연별 통계 차트
- 이메일 알림 시스템
- 간단한 계산기 도구

### 📋 Phase 3: 미구현
- 오픈뱅킹 API 연동 (카드/계좌 연동)
- 실시간 주가 데이터 연동
- 머신러닝 기반 고도화된 추천
- 커뮤니티 기능

---

## 🔧 문제 해결

### 🚨 자주 발생하는 문제

#### 1. 데이터베이스 연결 실패
```bash
# 해결책 1: Docker 컨테이너 재시작
docker-compose restart

# 해결책 2: 네트워크 확인
docker network ls
docker network inspect finmate-backend_default

# 해결책 3: 포트 충돌 확인
netstat -tulpn | grep :3306
```

#### 2. 빌드 실패
```bash
# 해결책 1: Gradle 캐시 정리
./gradlew clean

# 해결책 2: 의존성 다시 다운로드
./gradlew build --refresh-dependencies

# 해결책 3: Java 버전 확인
java -version  # Java 17 이상 필요
```

#### 3. 메모리 부족
```bash
# 해결책: JVM 메모리 설정
export GRADLE_OPTS="-Xmx2g -Xms1g"
./gradlew runServer
```

### 📊 로그 확인

```bash
# 애플리케이션 로그
tail -f logs/finmate.log

# MySQL 로그
docker logs finmate-mysql

# 실시간 로그 모니터링
docker-compose logs -f
```

---

## 🏆 만든 사람

<div align="center">

**FinMate Development Team**

[![GitHub](https://img.shields.io/badge/GitHub-mango606-black?style=flat&logo=github&labelColor=171717)](https://github.com/mango606)

</div>
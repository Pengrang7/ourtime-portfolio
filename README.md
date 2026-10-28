# OurTime

그룹 구성원이 함께한 장소와 추억을 지도에 기록하고 공유하는 풀스택 웹 서비스입니다.

개인 포트폴리오용 공개 저장소이며, 실제 인증정보와 사용자 데이터는 포함하지 않습니다.

## 핵심 기능

- JWT 기반 회원가입·로그인·토큰 갱신
- 그룹 생성과 이메일 초대, 관리자·멤버 권한 분리
- 네이버 지도 기반 추억 등록·조회
- 이미지 업로드와 AWS S3 연동
- 댓글, 좋아요, 태그, 기간·작성자 필터
- 과거 기록 리마인더 스케줄링
- 공통 예외 응답과 Swagger API 문서

## 기술 스택

### Backend

- Java 17, Spring Boot 3.2
- Spring Security, JWT
- Spring Data JPA, QueryDSL
- MySQL 8, H2
- AWS S3, Springdoc OpenAPI

### Frontend

- React 18, TypeScript
- React Query, React Hook Form
- Styled Components, Naver Maps API

### Development

- Gradle 8.5, Docker Compose
- JUnit 5, GitHub Actions

## 구조

```text
React
  └─ REST API / JWT
       └─ Spring Boot
            ├─ Controller
            ├─ Service
            ├─ Repository
            ├─ MySQL
            └─ AWS S3
```

주요 도메인은 `User`, `Group`, `UserGroup`, `Memory`, `Comment`, `Like`, `Tag`, `GroupInvitation`으로 구성했습니다. 사용자와 그룹의 N:M 관계는 `UserGroup`으로 분리하고 역할을 함께 관리합니다.

## 실행 방법

### 1. 환경변수 준비

```bash
cp env.example .env
```

`.env`의 다음 값을 로컬 환경에 맞게 변경합니다.

- `SPRING_DATASOURCE_*`
- `JWT_SECRET`
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_BUCKET_NAME`
- `REACT_APP_NAVER_MAP_CLIENT_ID`

운영 환경에서는 예제 값을 사용하지 않습니다. `JWT_SECRET`은 최소 32바이트의 임의 값으로 생성합니다.

### 2. 데이터베이스 실행

```bash
docker compose --env-file .env up -d mysql
```

### 3. 백엔드 실행

```bash
./gradlew bootRun
```

### 4. 프론트엔드 실행

```bash
cd frontend
npm ci
npm start
```

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Frontend: `http://localhost:3000`

## 검증

```bash
./gradlew test
cd frontend
npm ci
npm run build
```

GitHub Actions에서도 백엔드 테스트와 프론트엔드 프로덕션 빌드를 각각 검증합니다.

## 보안

- 실제 키와 비밀번호는 Git에 저장하지 않습니다.
- `.env`, `application-local.yml`, `frontend/src/config.local.ts`는 Git에서 제외합니다.
- 운영 설정에는 기본 비밀번호나 JWT secret fallback을 두지 않습니다.
- 공개 저장소에는 예제 데이터만 포함합니다.

## 문서

- [API 문서](API_DOCUMENTATION.md)

## 개선 예정

- Service 계층 단위 테스트와 권한 통합 테스트 확대
- Flyway 기반 스키마 마이그레이션 도입
- 배포 환경 분리와 모니터링 구성

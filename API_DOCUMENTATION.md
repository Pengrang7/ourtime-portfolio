# OurTime API Documentation

## 📋 목차
- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [API 엔드포인트](#api-엔드포인트)
- [데이터베이스 구조](#데이터베이스-구조)

## 프로젝트 소개

**OurTime**은 함께한 시간을 지도 위에 기록하고 공유하는 그룹형 추억 기록 서비스입니다.

### 핵심 개념
- 사용자는 여러 그룹에 속할 수 있습니다.
- 각 그룹별로 추억이 공유됩니다.
- 모든 추억은 지도 기반(위도, 경도)으로 기록됩니다.
- 그룹 타입: 커플, 가족, 친구, 팀 등

## 주요 기능

### 1️⃣ 사용자 관리
- 회원가입 / 로그인 (JWT 기반)
- 프로필 관리 (이미지, 닉네임, 이메일)
- 비밀번호 변경 / 회원 탈퇴

### 2️⃣ 그룹 관리
- 그룹 생성 및 관리
- 초대 코드 생성 및 공유
- 그룹 참여 / 탈퇴
- 역할 관리 (ADMIN, MEMBER)

### 3️⃣ 추억 기록
- 위치 정보 + 내용 + 사진 + 날짜 기록
- 태그 시스템
- 그룹 내 공유
- 지도 기반 조회

### 4️⃣ 소셜 기능
- 댓글 작성 / 수정 / 삭제
- 좋아요 기능
- 실시간 알림 (선택적)

### 5️⃣ 파일 업로드
- AWS S3 이미지 업로드
- 프로필, 그룹, 추억 이미지 지원

### 6️⃣ 알림 기능
- "오늘은 1년 전 이 날 💌" 리마인더
- 스케줄러 기반 알림

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.2.0 |
| Language | Java 17 |
| Database | MySQL 8.0 (Production), H2 (Development) |
| ORM | JPA (Hibernate) + QueryDSL |
| Security | Spring Security + JWT |
| File Storage | AWS S3 |
| Documentation | Springdoc OpenAPI (Swagger) |
| Build Tool | Gradle |

## 시작하기

### 사전 요구사항
- Java 17 이상
- MySQL 8.0 (선택사항: H2 사용 가능)
- AWS S3 계정 (이미지 업로드 사용 시)

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd ourtime
```

### 2. 데이터베이스 설정
MySQL 데이터베이스를 생성합니다:
```sql
CREATE DATABASE ourtime CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 환경 변수 설정
`application.yml` 파일을 수정하거나 환경 변수를 설정합니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ourtime
    username: your-username
    password: your-password

spring.cloud.aws:
  credentials:
    access-key: your-aws-access-key
    secret-key: your-aws-secret-key
  s3:
    bucket: your-bucket-name

jwt:
  secret: your-jwt-secret-key-minimum-256-bits
```

### 4. 빌드 및 실행
```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 또는
java -jar build/libs/ourtime-0.0.1-SNAPSHOT.jar
```

### 5. H2 개발 모드 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 6. API 문서 확인
애플리케이션 실행 후 다음 URL에서 Swagger UI를 확인할 수 있습니다:
```
http://localhost:8080/swagger-ui.html
```

## API 엔드포인트

### 🔐 인증 (Auth)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/refresh` | 토큰 갱신 |

### 👤 사용자 (User)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | 내 정보 조회 |
| GET | `/api/users/{userId}` | 사용자 정보 조회 |
| PUT | `/api/users/me` | 프로필 수정 |
| PUT | `/api/users/me/password` | 비밀번호 변경 |
| DELETE | `/api/users/me` | 회원 탈퇴 |

### 👥 그룹 (Group)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/groups` | 그룹 생성 |
| GET | `/api/groups` | 내 그룹 목록 조회 |
| GET | `/api/groups/{groupId}` | 그룹 상세 조회 |
| PUT | `/api/groups/{groupId}` | 그룹 정보 수정 |
| POST | `/api/groups/{groupId}/invite-code` | 초대 코드 재생성 |
| POST | `/api/groups/join` | 그룹 참여 |
| DELETE | `/api/groups/{groupId}/leave` | 그룹 탈퇴 |
| DELETE | `/api/groups/{groupId}` | 그룹 삭제 |

### 📷 추억 (Memory)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/memories` | 추억 생성 |
| GET | `/api/memories/{memoryId}` | 추억 상세 조회 |
| GET | `/api/memories/groups/{groupId}` | 그룹별 추억 목록 조회 |
| GET | `/api/memories/groups/{groupId}/tags/{tagId}` | 태그별 추억 조회 |
| PUT | `/api/memories/{memoryId}` | 추억 수정 |
| DELETE | `/api/memories/{memoryId}` | 추억 삭제 |

### 💬 댓글 (Comment)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/memories/{memoryId}/comments` | 댓글 작성 |
| GET | `/api/memories/{memoryId}/comments` | 댓글 목록 조회 |
| PUT | `/api/memories/{memoryId}/comments/{commentId}` | 댓글 수정 |
| DELETE | `/api/memories/{memoryId}/comments/{commentId}` | 댓글 삭제 |

### ❤️ 좋아요 (Like)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/memories/{memoryId}/likes` | 좋아요 토글 |
| GET | `/api/memories/{memoryId}/likes/count` | 좋아요 개수 조회 |
| GET | `/api/memories/{memoryId}/likes/me` | 좋아요 상태 확인 |

### 🏷️ 태그 (Tag)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tags` | 태그 생성 |
| GET | `/api/tags` | 태그 목록 조회 |
| GET | `/api/tags/{tagId}` | 태그 상세 조회 |
| DELETE | `/api/tags/{tagId}` | 태그 삭제 |

### 📁 파일 (File)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/profile` | 프로필 이미지 업로드 |
| POST | `/api/files/group` | 그룹 이미지 업로드 |
| POST | `/api/files/memory` | 추억 이미지 업로드 |
| DELETE | `/api/files` | 파일 삭제 |

### 🏥 헬스 체크
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | 서버 상태 확인 |

## 데이터베이스 구조

### ERD
```
User (1) ──< (N) UserGroup (N) >── (1) Group
                             │
                             │ 1:N
                             ▼
                           Memory ──< MemoryTag >── Tag
                             │
                             ├─< Comment
                             └─< Like
```

### 주요 테이블

#### users
- id (PK)
- email (UNIQUE)
- password
- nickname
- profile_image
- created_at, updated_at

#### groups
- id (PK)
- name
- type (ENUM)
- invite_code (UNIQUE)
- description
- group_image
- created_by
- created_at, updated_at

#### user_group
- id (PK)
- user_id (FK)
- group_id (FK)
- role (ENUM: ADMIN, MEMBER)
- created_at, updated_at

#### memories
- id (PK)
- group_id (FK)
- user_id (FK)
- title
- description
- latitude, longitude
- location_name
- visited_at
- created_at, updated_at

#### tags
- id (PK)
- name (UNIQUE)
- color
- created_at, updated_at

#### comments
- id (PK)
- memory_id (FK)
- user_id (FK)
- content
- created_at, updated_at

#### likes
- id (PK)
- memory_id (FK)
- user_id (FK)
- created_at

## 인증 방식

### JWT 토큰
API는 JWT (JSON Web Token) 기반 인증을 사용합니다.

1. 로그인 후 `accessToken`과 `refreshToken`을 받습니다.
2. 모든 인증이 필요한 요청의 헤더에 토큰을 포함합니다:
   ```
   Authorization: Bearer {accessToken}
   ```
3. `accessToken`이 만료되면 `refreshToken`으로 갱신합니다.

## 에러 응답 형식

```json
{
  "code": "U001",
  "message": "사용자를 찾을 수 없습니다.",
  "errors": [],
  "timestamp": "2025-01-01T12:00:00"
}
```

## 성공 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "message": "성공 메시지",
  "timestamp": "2025-01-01T12:00:00"
}
```

## 개발 팁

### H2 콘솔 접속
개발 모드에서 H2 데이터베이스 콘솔에 접속:
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
Username: sa
Password: (비워두기)
```

### 로그 레벨 조정
`application.yml`에서 로그 레벨을 조정할 수 있습니다:
```yaml
logging:
  level:
    com.ourtime: debug
```

### 스케줄러 설정
`MemoryReminderScheduler`에서 알림 시간을 조정할 수 있습니다:
```java
@Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
```

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 기여

Pull Request는 언제나 환영합니다!

## 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 등록해주세요.

# Place Board

네이버 지도 등에서 공유받은 개별 장소 링크를 한곳에서 관리하기 위한 웹 애플리케이션입니다.

현재 `dev` 브랜치에는 공동 보드 MVP가 구현되어 있습니다.

- Spring Boot와 React/Vite 기본 프로젝트
- MySQL 연결 및 Flyway 초기화
- Backend/Frontend 연결 확인 API
- NAVER·KAKAO·GOOGLE 장소 링크 검증 및 단축 URL 리다이렉트 추적
- 링크만으로 장소명, 주소, 좌표, 지역, 카테고리, 전화번호 자동 입력
- 장소 확인 후 MySQL 저장 및 Provider ID 중복 방지
- 비활성 장소 재등록 시 기존 데이터 재활성화
- Google 로그인(세션 쿠키)과 로그아웃
- 비공개 보드와 이메일 기반 멤버 초대
- 지역, 태그, 보드 전체 재방문 의사 필터
- 사용자별 방문 기록(○ 있음 / △ 고민 / × 안 감, 후기, 방문일)
- 목록|지도 한 화면 표시 (목록에서 장소를 누르면 지도가 해당 위치로 이동)

Discord, 사진 기능은 아직 구현하지 않았습니다.

## 기술 스택

- Backend: Java 25, Spring Boot 4.1, Maven, MyBatis, Flyway
- Frontend: React, Vite, TypeScript, Axios, TanStack Query
- Database: MySQL 8.x

## 로컬 실행 준비

- JDK 25 최신 보안 패치(Oracle JDK 사용 시 25.0.5.0.2 이상)
- Node.js 24.22.1 이상(24.x)
- Aiven MySQL 8.4 서비스와 `placeboard` 데이터베이스
- IntelliJ IDEA

로컬 실행에는 Docker가 필요하지 않습니다. Backend는 환경변수로 지정한 Aiven MySQL에 연결합니다.

### 1. Aiven MySQL 연결 정보 준비

Aiven Console의 Connection information에서 Host, Port, User, Password를 확인합니다. 실제 접속 정보는 파일에 저장하거나 Git에 커밋하지 않습니다.

```text
DB_URL=jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/<AIVEN_DATABASE>?sslMode=REQUIRED
DB_USER=<AIVEN_USER>
DB_PASSWORD=<AIVEN_PASSWORD>
```

`<AIVEN_DATABASE>`에는 `placeboard`를 넣습니다. 애플리케이션 시작 시 Flyway가 선택한 데이터베이스에 migration을 자동 적용합니다.

### 2. IntelliJ에서 Backend 실행

1. IntelliJ에서 프로젝트 루트 또는 `backend/pom.xml`을 엽니다.
2. Project SDK와 Maven Runner JDK를 Java 17로 설정합니다.
3. Run Configuration의 Main class를 아래 값으로 설정합니다.

```text
com.placeboard.PlaceBoardApplication
```

4. Working directory를 `C:\Users\user\IdeaProjects\place-board\backend`로 설정합니다.
5. Environment variables에 아래 값을 한 줄로 입력합니다.

```text
DB_URL=jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/<AIVEN_DATABASE>?sslMode=REQUIRED;DB_USER=<AIVEN_USER>;DB_PASSWORD=<AIVEN_PASSWORD>
```

`PORT`는 생략하면 `8080`을 사용합니다. 다른 포트가 필요하면 같은 Environment variables에 `PORT=<포트>`를 추가합니다.

6. 실행 로그에서 HikariCP 시작 및 Flyway migration 성공을 확인한 후 `http://localhost:8080/api/health`에서 응답을 확인합니다.

```json
{"status":"UP"}
```

### 3. Frontend 실행

IntelliJ Terminal에서 실행합니다.

```powershell
cd C:\Users\user\IdeaProjects\place-board\frontend
npm ci
npm run dev
```

브라우저에서 `http://localhost:5173`에 접속합니다. 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

## Google Cloud Run Backend 배포 설정

Cloud Run에는 모노레포 전체가 아니라 `backend` 디렉터리만 소스로 전달합니다. 저장소에는 실제 Aiven 접속정보나 Secret을 저장하지 않습니다.

### 1. 소스 배포 및 빌드 설정

프로젝트 루트에서 다음 명령으로 배포합니다. Cloud Run 소스 배포가 `backend/project.toml`을 기준으로 Google `google-24/java25` 런타임을 사용해 애플리케이션을 빌드합니다.

```powershell
gcloud run deploy place-board-backend `
  --source backend `
  --region YOUR_REGION `
  --memory 512Mi `
  --set-env-vars "DB_URL=jdbc:mysql://YOUR_AIVEN_HOST:PORT/placeboard?sslMode=REQUIRED,DB_USER=YOUR_DB_USER" `
  --set-secrets "DB_PASSWORD=YOUR_SECRET_NAME:latest" `
  --allow-unauthenticated
```

기존 서비스에 다른 일반 환경변수가 있다면 `--set-env-vars`가 이를 제거할 수 있으므로, Google Cloud Console에서 개별 추가하거나 `--update-env-vars`를 사용합니다.

### 2. 환경변수와 Secret

Cloud Run 서비스의 **Variables & Secrets**에 다음 값을 설정합니다.

| 이름 | 값 | 저장 방식 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/placeboard?sslMode=REQUIRED` | 일반 환경변수 |
| `DB_USER` | Aiven 사용자명 | 환경변수 |
| `DB_PASSWORD` | Aiven 비밀번호 | Secret Manager Secret을 환경변수로 연결 |

Cloud Run 런타임 서비스 계정에는 해당 Secret의 **Secret Manager Secret Accessor** 권한이 필요합니다. 애플리케이션은 Google Cloud SDK를 호출하지 않고 Cloud Run이 주입한 `${DB_PASSWORD}`를 읽습니다. `DB_PASSWORD`를 일반 텍스트 환경변수나 GitHub 저장소에 저장하지 않습니다.

### 3. 포트와 Health Check

- Container port: `8080` (Cloud Run이 같은 값을 `PORT`로 자동 주입)
- Startup probe: `HTTP GET /api/health`
- 첫 기동 시 Flyway 실행 시간을 고려해 startup probe의 초기 지연/실패 허용 시간을 충분히 설정합니다.

`application.yml`의 `server.port: ${PORT:8080}` 설정 때문에 Cloud Run에서는 주입된 포트로 listen하고, 로컬에서는 `PORT`가 없으면 `8080`을 사용합니다. Cloud Run에 `PORT`를 직접 등록하지 않습니다.

배포 로그에서 아래 순서가 확인되어야 정상입니다.

```text
HikariPool-1 - Start completed
Successfully validated ... migrations
Schema ... is up to date
Tomcat started on port ...
```

Flyway 오류가 발생하면 새 배포가 시작되지 않으므로 Aiven 서비스 상태, JDBC URL의 데이터베이스명, Secret 연결 여부부터 확인합니다.

### 4. Aiven 무료 MySQL keepalive

무료 MySQL의 비활성 자동 종료를 줄이기 위해 서버가 기동된 뒤 1분 후부터 50분마다 `SELECT 1`을 실행합니다.

- 구현: Spring `@Scheduled`
- 주기: 50분(`placeboard.keepalive.interval-ms=3000000`)
- 실패 처리: 예외와 stack trace를 서버 로그에 기록하고 다음 주기에 재시도
- 외부 keepalive endpoint와 토큰은 사용하지 않음

Cloud Run에서 요청이 없을 때도 스케줄러가 실행되려면 서비스 수준 최소 인스턴스를 `1`로 설정하고 CPU 상시 할당(인스턴스 기반 과금)을 사용해야 합니다.

```bash
gcloud run services update place-board-api \
  --region asia-northeast3 \
  --min 1 \
  --no-cpu-throttling
```

> 이 로직은 이미 꺼진 Aiven 서비스를 켜지는 못합니다. Aiven을 먼저 수동으로 켜야 하며, Aiven이 비활성 판단 기준을 공개하지 않으므로 자동 종료 방지를 보장하지는 않습니다.

현재 로그인 세션과 보드 SSE 구독은 애플리케이션 메모리에 저장됩니다. Cloud Run 인스턴스를 여러 개 사용하면 세션과 보드 변경 이벤트가 인스턴스 간 공유되지 않으므로, 초기 배포는 최대 인스턴스 1개로 운영하거나 운영 확장 전에 외부 세션 저장소와 Redis Pub/Sub 같은 이벤트 브로커 도입을 검토해야 합니다.

## 현재 API

### 연결 확인

```http
GET /api/health
```

### 장소 링크 미리보기

```http
POST /api/places/preview
Content-Type: application/json

{
  "url": "https://naver.me/GXApFSiE"
}
```

성공 응답:

```json
{
  "provider": "NAVER",
  "providerPlaceId": "1610498965",
  "originalUrl": "https://naver.me/GXApFSiE",
  "name": "티원카페&아레나",
  "address": "서울 강남구 선릉로115길 4 1층",
  "latitude": 37.5124774,
  "longitude": 127.0426383,
  "country": "KR",
  "sido": "서울특별시",
  "sigungu": "강남구",
  "dong": "역삼동",
  "category": "카페,디저트",
  "phone": null
}
```

지원 링크:

| Provider | 예시 |
| --- | --- |
| NAVER | `naver.me/…`, `map.naver.com/p/entry/place/{id}`, `m.place.naver.com/{업종}/{id}` |
| KAKAO | `kko.kakao.com/…`, `kko.to/…`, `place.map.kakao.com/{id}`, `map.kakao.com/?itemId={id}` |
| GOOGLE | `maps.app.goo.gl/…`, `google.com/maps/place/…` |

오류 응답:

```json
{
  "code": "UNSUPPORTED_MAP_URL",
  "message": "지원하지 않는 지도 URL입니다."
}
```

장소 정보는 각 지도 서비스의 공개 장소 페이지가 내려주는 데이터(NAVER 모바일 플레이스 페이지, 카카오맵 장소 패널, 구글 지도 장소 미리보기)에서 읽습니다. 공식 API가 아니므로 서비스 측 페이지 구조가 바뀌거나 요청이 차단되면 미리보기가 실패할 수 있으며, 이때는 오류 메시지가 표시됩니다. 사용자가 직접 입력하는 값은 태그와 방문 기록입니다.

## Google 로그인 설정

### 1. Google Cloud Console

1. https://console.cloud.google.com 에서 프로젝트를 만듭니다.
2. **API 및 서비스 → OAuth 동의 화면**: User Type **외부**, 게시 상태 **테스트**로 두고 **테스트 사용자**에 로그인할 구글 계정을 추가합니다. 테스트 모드에서는 여기 등록한 계정만 로그인할 수 있습니다.
3. **사용자 인증 정보 → OAuth 클라이언트 ID 만들기**: 유형 **웹 애플리케이션**, **승인된 JavaScript 원본**에 `http://localhost:5173`을 넣습니다. 리디렉션 URI와 클라이언트 보안 비밀번호는 쓰지 않습니다.

### 2. Backend 환경변수

발급된 클라이언트 ID(`….apps.googleusercontent.com`)를 Backend 실행 환경변수 `GOOGLE_CLIENT_ID`로 넣고 재시작합니다. IntelliJ에서는 Run Configuration → Environment variables에 넣습니다. Frontend는 `/api/auth/config`로 이 값을 받아 로그인 버튼을 그리므로 따로 설정하지 않습니다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | (없음) | Google OAuth 웹 클라이언트 ID |
| `PLACEBOARD_HEADER_LOGIN` | `false` | `true`면 로그인 없이 `owner@local.place`(또는 `X-User-Email` 헤더)로 접속하는 로컬 개발 모드. 운영에서는 켜지 마세요. |

로그인하면 Backend가 구글 ID 토큰의 서명·발급자·만료·aud를 검증한 뒤 세션 쿠키(httpOnly, SameSite=Lax, 14일)를 발급합니다. 세션은 서버 메모리에 있으므로 Backend를 재시작하면 다시 로그인해야 합니다.

## Google Calendar 자동 연동 설정

일정 화면의 **Google Calendar 연동** 버튼을 한 번 승인하면, 이후 해당 사용자가 접근할 수 있는 공유 보드 일정의 생성·수정·삭제가 기본 Google Calendar에 자동 반영됩니다. 공유 멤버별 Google Calendar에 표시하려면 각 멤버가 자신의 계정으로 연동해야 합니다.

생리 주기 기록도 공유 보드 구성원 중 Google Calendar를 연동한 모든 사용자의 캘린더에 함께 반영됩니다. 기록 작성자가 Google을 연동하지 않았어도 다른 연동 사용자의 캘린더에는 표시됩니다. 실제 생리 기간(🌸), 앱이 계산한 생리 예정(🌸 예정, 앞으로 6주기), 가임기(🌱), 배란 예정(🌼)이 `닉네임 + 이모지` 형식의 알림 없는 비공개(`private`)·한가함(`transparent`) 종일 일정으로 올라갑니다. 기록을 추가·수정·삭제하면 영향받는 사용자들의 생리 주기 일정 전체를 다시 계산해 바뀐 구간만 지우고 새로 만듭니다(`cycle_google_event`). 예측 규칙은 프론트엔드 `cycleCalendar.ts`와 백엔드 `CycleCalendarPlanner`가 같아야 합니다.

1. Google Cloud Console에서 **Google Calendar API**를 사용 설정합니다.
2. **Google Auth Platform → 데이터 액세스 → 범위 추가 또는 삭제**에서 아래 범위를 추가합니다.

```text
https://www.googleapis.com/auth/calendar.events
```

3. 기존 웹 OAuth 클라이언트의 **승인된 리디렉션 URI**에 실행 환경별 주소를 추가합니다.

```text
# 로컬(Vite 프록시를 통해 같은 origin으로 세션 유지)
http://localhost:5173/api/google-calendar/oauth/callback

# 운영
https://YOUR_FRONTEND_DOMAIN/api/google-calendar/oauth/callback
```

4. Backend에 아래 값을 설정합니다. Client Secret과 암호화 키는 Git이나 Frontend 환경변수에 넣지 않습니다.

| 환경변수 | 설명 |
| --- | --- |
| `GOOGLE_CLIENT_SECRET` | `GOOGLE_CLIENT_ID`와 같은 웹 OAuth 클라이언트의 보안 비밀번호 |
| `GOOGLE_CALENDAR_REDIRECT_URI` | 현재 환경에 등록한 정확한 callback URI |
| `GOOGLE_TOKEN_ENCRYPTION_KEY` | refresh token 암호화용 Base64 32바이트 키 |

PowerShell에서 암호화 키를 한 번 생성하는 예시입니다. 출력값은 Secret Manager에 바로 저장하고 소스 파일에는 기록하지 않습니다.

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Calendar API 장애는 내부 일정 저장을 취소하지 않으며 `scheduleId`와 작업 종류를 오류 로그에 남깁니다. 연결 해제 후에도 이미 Google Calendar에 생성된 일정은 사용자가 직접 관리할 수 있도록 남겨 둡니다.

## 공동 보드 사용

1. 보드 소유자가 상단 보드 메뉴(▾)에서 보드를 생성합니다.
2. **멤버 초대**에서 친구의 구글 계정 이메일과 권한을 등록합니다.
3. 친구가 그 구글 계정으로 로그인하면 초대에 연결되고, 처음 한 번 보드에서 쓸 이름을 입력합니다.
4. 기본 멤버는 보드 조회, 장소 추가, 본인 방문 기록 작성을 할 수 있습니다.
5. 장소에 기존 태그를 붙이고 떼는 것은 모든 멤버가 할 수 있습니다.
6. 장소 삭제, 원본 정보 수정, 태그 생성·수정·삭제, 멤버 초대는 소유자 또는 권한이 부여된 멤버만 가능합니다. 보드 이름 수정은 소유자만 가능합니다.

## 지도 화면 연동에 필요한 것

### 현재 URL 판별 기능

현재 구현된 URL 검증, 단축 URL 리다이렉트, Place ID 추출에는 NAVER API 키가 필요하지 않습니다.

### NAVER 지도 화면 표시

지도 자체를 화면에 표시하려면 NAVER Cloud Platform에서 Maps 애플리케이션을 등록해야 합니다.

1. [NAVER Cloud Platform Console](https://console.ncloud.com/)에 로그인합니다.
2. **Services > Application Services > Maps > Application**에서 애플리케이션을 등록합니다.
3. **Dynamic Map**을 선택합니다.
4. Web 서비스 URL에 로컬 주소와 운영 도메인을 등록합니다.
5. 발급된 Client ID(`ncpKeyId`)를 사용합니다.

Frontend 환경변수 예시:

```text
# frontend/.env.local
VITE_NAVER_MAP_CLIENT_ID=<NAVER Maps Client ID>
```

`frontend/.env.local`은 Git에 올리지 않습니다. Client Secret이 필요한 서버 API를 추가할 경우 Secret은 Backend 환경변수로만 관리하며 `VITE_` 변수에 넣으면 안 됩니다.

공식 문서:

- [NAVER Maps Client ID 발급](https://navermaps.github.io/maps.js.en/docs/tutorial-1-Getting-Client-ID.html)
- [NAVER Maps JavaScript API 시작](https://navermaps.github.io/maps.js.en/docs/tutorial-2-Getting-Started.html)

환경변수를 설정한 뒤 Frontend를 재시작하면 메인 화면의 지도 컴포넌트가 Client ID를 읽어 NAVER 지도를 표시합니다.

### Google 지도 화면 표시

해외 장소는 Google Maps JavaScript API로 표시합니다.

#### 무료 체험 및 과금

- 신규 계정의 USD 300 무료 체험 크레딧은 기간 또는 크레딧이 소진될 때까지만 적용됩니다.
- 별도의 월 USD 100 구독 요금제는 신청하지 않아도 되며, 이 프로젝트는 사용량 기반 결제(Pay as you go)로 사용할 수 있습니다.
- Maps JavaScript API의 Dynamic Maps는 매월 10,000회 지도 로드까지 무료이고, 초과 사용량은 과금됩니다. 지도 확대·축소와 이동 자체는 별도 지도 로드로 계산되지 않습니다.
- 무료 체험 중에도 결제 계정 연결과 API 키가 필요합니다. 체험 종료 후 계속 사용할지는 Google Cloud 결제 계정 상태와 전환 설정을 확인합니다.

가격과 프로모션 조건은 변경될 수 있으므로 배포 전 [Google Maps 공식 요금표](https://developers.google.com/maps/billing-and-pricing/pricing)와 콘솔에 표시된 체험 조건을 다시 확인합니다.

#### API 및 키 설정

1. [Google Cloud Console](https://console.cloud.google.com/)에서 프로젝트를 선택합니다.
2. **Maps JavaScript API**와 **Places API (New)**를 사용 설정하고 결제 계정을 연결합니다. Places API는 해외 장소의 사진을 표시할 때 사용합니다.
3. **API 및 서비스 > 사용자 인증 정보 > 사용자 인증 정보 만들기 > API 키**에서 새 키를 만듭니다.
4. **애플리케이션 제한사항**을 `웹사이트`로 선택하고 아래 HTTP referrer를 등록합니다.

```text
http://localhost:5173
http://localhost:5173/*
http://127.0.0.1:5173
http://127.0.0.1:5173/*
https://<운영 도메인>
https://<운영 도메인>/*
# 현재 배포 주소 예시
https://YOUR_FRONTEND_DOMAIN
https://YOUR_FRONTEND_DOMAIN/*
```

5. **API 제한사항**을 `키 제한`으로 선택하고 `Maps JavaScript API`, `Places API (New)`만 허용합니다.
6. 저장 후 제한사항이 반영된 키만 `frontend/.env.local`에 넣습니다.

```text
# frontend/.env.local
VITE_NAVER_MAP_CLIENT_ID=<NAVER Maps Client ID>
VITE_GOOGLE_MAPS_API_KEY=<Google Maps API Key>
```

API 키를 소스 코드, README, 이슈, 채팅 등에 붙여 넣지 않습니다. 제한 전에 노출된 키는 수정해서 재사용하지 말고 즉시 삭제한 뒤 새 키를 발급합니다.

#### 과금 사고 방지

개인 개발 환경에서는 **Google Maps Platform > 할당량(Quotas) > Maps JavaScript API**에서 다음처럼 낮게 시작하고 실제 트래픽에 맞춰 조정합니다.

```text
분당 프로젝트 지도 로드: 10
분당 IP별 지도 로드: 5
```

할당량이 너무 낮으면 정상 사용자에게도 지도가 표시되지 않을 수 있습니다. 또한 **결제 > 예산 및 알림**에서 이 프로젝트만 대상으로 월 USD 1~5 예산과 50%, 90%, 100% 알림을 설정합니다. 예산 알림은 통지만 하고 사용량이나 과금을 자동 차단하지 않으므로 API 키 제한과 할당량을 반드시 함께 설정합니다.

보드 생성 시 국내 보드(NAVER 지도) 또는 해외 보드(Google 지도)를 선택합니다. 기존 보드는 migration에서 국내 보드로 유지됩니다.

`RefererNotAllowedMapError`가 나오면 오류에 표시된 사이트 주소의 루트와 `/*` 패턴을 위 HTTP referrer 목록에 모두 등록한 뒤 제한사항 전파를 기다립니다. 이는 프런트 코드가 아니라 Google Cloud API 키 제한 설정 문제입니다.

Google Maps 브라우저 API 키는 프런트엔드에서 보이는 값이므로, 반드시 HTTP referrer와 API 종류 제한을 함께 설정해야 합니다.

해외 장소의 평점·리뷰 수·영업시간·전화·웹사이트는 공개 장소 페이지에서 읽고, 사진만 Places API (New)의 Place Details `photos` 필드로 조회합니다. Places API가 꺼져 있거나 할당량을 초과하면 기본 장소 정보는 유지되고 사진 탭에 오류가 표시됩니다. Google Places는 식당의 개별 메뉴명·가격을 제공하지 않으므로 메뉴 탭은 공식 웹사이트와 Google 지도 링크를 제공합니다.

공식 문서:

- [Google Maps JavaScript API 로드](https://developers.google.com/maps/documentation/javascript/load-maps-js-api)
- [Google Place Details 및 사진](https://developers.google.com/maps/documentation/javascript/place-details)
- [Google Maps API 키 권장사항](https://developers.google.com/maps/api-security-best-practices)
- [Google Maps 비용 관리](https://developers.google.com/maps/billing-and-pricing/manage-costs)

### Place ID로 장소 상세정보 가져오기

NAVER·KAKAO·GOOGLE 모두 Place ID로 상세정보를 반환하는 공개 API가 없어, 링크 미리보기는 각 서비스의 공개 장소 페이지 데이터를 읽는 방식으로 구현했습니다. 별도 API 키는 필요하지 않습니다. 구조 변경에 대비해 각 Provider 파싱은 `backend/src/main/java/com/placeboard/map/`에 분리되어 있습니다.

## 장소 저장 및 리스트

리스트는 NAVER 지도 API가 아니라 Place Board의 MySQL에서 조회하며 React Query로 갱신합니다.

```text
장소 추가 모달에 지도 링크 입력
  -> POST /api/places/preview (장소 정보 자동 조회)
  -> 태그 선택
  -> POST /api/boards/{boardId}/places (장소 + 태그 저장)
  -> MySQL 저장
  -> GET /api/boards/{boardId}/places
  -> React Query 조회
  -> 장소 카드 목록 렌더링
```

주요 DB 구조:

- `app_user`: 사용자
- `board`: 보드 정보
- `board_member`: 초대 멤버 및 세부 권한
- `place`: 장소명, 주소, 좌표 등 공통 장소 정보
- `place_source`: Provider, 원본 URL, NAVER Place ID
- `board_place`: 보드와 장소 연결, 추가자, use_yn
- `tag`, `place_tag`: 보드별 태그
- `place_review`: 사용자별 재방문 의사, 후기, 방문일

기본 스키마는 `V2__create_collaboration_schema.sql`에서 생성되고 이후 변경은 순차 Flyway migration으로 적용됩니다.

### 주요 API

```text
GET    /api/users/me
GET    /api/boards
POST   /api/boards
DELETE /api/boards/{boardId}
GET    /api/boards/{boardId}/members
POST   /api/boards/{boardId}/members
PATCH  /api/boards/{boardId}/members/{memberUserId}
GET    /api/boards/{boardId}/cycle-periods
POST   /api/boards/{boardId}/places
GET    /api/boards/{boardId}/places?revisit=&sido=&tagId=
PATCH  /api/boards/{boardId}/places/{boardPlaceId}
DELETE /api/boards/{boardId}/places/{boardPlaceId}
PATCH  /api/places/{placeId}?boardId=
GET    /api/boards/{boardId}/tags
POST   /api/boards/{boardId}/tags
PATCH  /api/boards/{boardId}/tags/{tagId}
DELETE /api/boards/{boardId}/tags/{tagId}
POST   /api/boards/{boardId}/places/{boardPlaceId}/tags/{tagId}
GET    /api/boards/{boardId}/places/{boardPlaceId}/reviews
PUT    /api/boards/{boardId}/places/{boardPlaceId}/reviews/me
DELETE /api/boards/{boardId}/places/{boardPlaceId}/reviews/me
```

## 명령행 실행

### Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### Frontend

```powershell
cd frontend
npm ci
npm run dev
```

## 검증

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run build
```

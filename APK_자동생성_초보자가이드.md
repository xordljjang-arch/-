# 🌊 갈맷길 백세만보 — 안드로이드 설치 파일(.apk) 만들기 가이드 (초보자용)

코딩을 전혀 몰라도, 컴퓨터에 무거운 프로그램을 설치하지 않아도 **무료 GitHub 자동 빌드 기능**을 이용해 **클릭 몇 번으로 안드로이드 휴대폰 설치 파일(`.apk`)**을 만드실 수 있도록 모든 설정이 완료되어 있습니다!

---

## 🚀 방법 1. 컴퓨터에 프로그램 설치 없이 무료로 `.apk` 파일 받기 (가장 추천!)

이 폴더 안에는 `.github/workflows/build-apk.yml` 이라는 **자동 APK 생성 로봇 설정 파일**이 들어 있습니다.

### 따라하기 순서 (약 3~5분 소요)
1. **압축 풀기**: 다운로드하신 `busan_galmaetgil_android_project.zip` 파일의 압축을 풀면 나오는 폴더 안의 파일들을 확인합니다.
2. **GitHub 무료 가입 및 로그인**: 인터넷 창에서 [https://github.com](https://github.com) 에 접속해 무료 회원가입 후 로그인합니다.
3. **새 저장소(바구니) 만들기**:
   - 우측 상단 **`+`** 아이콘 클릭 → **`New repository`** 클릭
   - **Repository name** 칸에 영어로 `galmaetgil-app` 입력
   - 화면 맨 아래 초록색 **`Create repository`** 버튼 클릭
4. **파일 통째로 올리기**:
   - 파란색 글씨 **`uploading an existing file`** 클릭
   - 압축을 풀어둔 폴더 안의 모든 파일과 폴더(`.github`, `app`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`)를 마우스로 드래그해서 네모 칸에 끌어다 놓습니다.
   - 업로드가 끝나면 아래 초록색 **`Commit changes`** 버튼 클릭!
5. **완성된 `.apk` 파일 다운로드하기**:
   - 화면 상단 메뉴 중 **`Actions`** 탭을 클릭합니다.
   - **`Build Galmaetgil Senior Pedometer APK`** 항목이 노란불(제작 중)에서 **초록색 체크(✅ 완성)**로 바뀔 때까지 약 2~3분 기다립니다.
   - 초록색 체크가 뜬 항목을 클릭한 뒤, 화면 맨 아래 **`Artifacts`** 칸에 있는 **`Galmaetgil-Senior-Pedometer-APK`**를 클릭하면 내 휴대폰에 바로 설치할 수 있는 `.apk` 파일이 다운로드됩니다!
   - 다운로드한 `.apk` 파일을 카카오톡 '나와의 채팅'이나 케이블로 안드로이드 휴대폰에 보내서 터치하면 바로 설치됩니다.

---

## 💻 방법 2. 내 컴퓨터의 Android Studio에서 직접 열기

만약 컴퓨터에 **Android Studio(안드로이드 스튜디오)**를 설치해서 직접 수정·빌드하고 싶으시다면:
1. Android Studio 실행 → **`Open`** 클릭 → 압축 푼 폴더 선택
2. 하단 Gradle 동기화가 끝날 때까지 대기 (약 1~2분)
3. 상단 메뉴 **`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`** 클릭
4. 우측 하단에 뜨는 **`locate`**를 누르면 `app-debug.apk` 파일이 바로 나옵니다!

---

## 🛠️ 프로젝트 핵심 소스 코드 구성

| 파일 경로 | 설명 |
| :--- | :--- |
| `app/src/main/AndroidManifest.xml` | 만보기 센서(`ACTIVITY_RECOGNITION`), 정밀 GPS(`ACCESS_FINE_LOCATION`), 인터넷 권한 설정 |
| `app/src/main/java/.../MainActivity.kt` | 어르신 맞춤형 큰 글씨 UI(WebView) + 안드로이드 하드웨어 만보기 센서·GPS·TTS 음성합성 브릿지 |
| `app/src/main/java/.../GalmaetgilGpsTracker.kt` | 부산 갈맷길 9코스 주요 구간 GPS 체크포인트(반경 50m) 추적 및 **70% 이상 통과 시 완주 도장 자동 발급** |
| `app/src/main/java/.../AntiCheatDetector.kt` | **자동걷기 기계(무휴식 일정 속도 흔들기)** 및 **심야 비현실적 시간대(01~05시) 집중 걸음** 실시간 차단 알고리즘 |
| `app/src/main/assets/index.html` | 어르신 맞춤형 화면 디자인 및 인터랙티브 스탬프북/공정 순위표 UI |

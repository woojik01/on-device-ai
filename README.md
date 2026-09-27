# on-device-ai

온디바이스 AI 캐릭터 Android 앱. 모든 대화·기억·상태는 최대한 기기 내부에서 처리·저장한다.

PRD 문서는 [`docs/prd/`](docs/prd/) 참조.

## 현재 단계

- ~~PRD-01 Android 앱 기반~~ — 완료 (S22 실기 확인 완료)
- **PRD-02 대화 엔진** — 진행 중
  - `ChatModel` 모델 추상화 + 스트리밍 응답/취소/오류 처리 (완료)
  - 로컬 런타임 1차 후보 **MediaPipe LLM Inference** 통합 완료 (`MediaPipeModel`)
  - 모델 파일이 기기에 있으면 로컬 추론, 없으면 개발용 `EchoModel` 폴백 ([모델 배치 가이드](docs/model-setup.md))
  - 남은 작업: S22 실측 벤치마크 → 최종 모델 확정 ([후보/벤치마크 계획](docs/model-candidates.md))

## 기술 스택

- Kotlin, Jetpack Compose, Material 3
- ViewModel 기반 상태 관리, 패키지별 기능 분리 (`data` / `ui` / `domain`)
- 로컬 저장 계층 추상화 (`ChatStore`, `AppPreferences` — 이후 SQLite로 교체 가능)
- 모델 추상화 (`ChatModel`) + MediaPipe LLM Inference 기반 로컬 추론 (`tasks-genai`)
- AGP 8.5 / Gradle 8.7 / JDK 17

## 빌드

```bash
gradle testDebugUnitTest   # 단위 테스트
gradle assembleDebug       # debug APK 빌드
```

GitHub Actions(`.github/workflows/android-ci.yml`)가 push/PR마다 단위 테스트와 APK 빌드를 실행하고, APK를 아티팩트로 업로드한다. 이 APK를 Galaxy S22에 설치해 실기 검증한다.

## 기준 기기

- 1차 기준: Samsung Galaxy S22
- 실제 테스트 기기의 Android 버전은 확인 후 여기에 기록한다: _(테스트 예정)_

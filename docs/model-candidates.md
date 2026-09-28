# 로컬 모델 후보 및 벤치마크 계획 (PRD-02)

## 원칙

- 후보 모델은 S22에서 성능·메모리·품질을 실측한 후 확정한다. (PRD-00: 문서만 보고 최종 모델을 고정하지 않는다)
- 개발용 클라우드 모델은 최종 앱의 필수 의존성이 아니다.
- EchoModel은 모델 미배치 시의 개발용 폴백이다.

## 교체 지점

```kotlin
// OnDeviceAiApplication.kt (ServiceLocator)
// files/models/의 모델을 확장자에 따라 런타임에 연결한다.
// .litertlm -> LiteRtModel, .task/.bin/.gguf -> MediaPipeModel, 없으면 EchoModel
```

UI·엔진 코드 변경 없이 구현체만 교체하면 된다.

## 런타임

| 런타임 | 특징 | 상태 |
|---|---|---|
| LiteRT-LM (litertlm-android 0.16.1) | Google 공식 차세대 런타임, .litertlm 지원, GPU/NPU 가속, Chrome·Pixel Watch 등 제품 적용 | **1차 후보** — 앱 내 통합 완료 (LiteRtModel), 설정 화면 자동 다운로드 |
| MediaPipe LLM Inference (tasks-genai 0.10.35) | Google 공식, .task/.bin/.gguf 지원 | 2차 후보 — 앱 내 통합 완료 (MediaPipeModel). MediaPipe GenAI는 deprecated 상태이며 LiteRT-LM 마이그레이션 권장 |
| llama.cpp (JNI) | 광범위한 모델 지원, 양자화 | 예비 (빌드 복잡도 높음) |
| MLC LLM | GPU 가속(Vulkan), 컴파일 기반 | 예비 (모델별 사전 컴파일 필요) |

## 후보 모델

| 모델 | 형식 | 크기 | 상태 |
|---|---|---|---|
| **Gemma 4 E2B IT** (litert-community, Apache-2.0) | .litertlm | 약 2.6GB | **1차 실측 대상** — 로그인 없이 다운로드 가능(게이트 없음), 앱 내 자동 다운로드 지원 |
| Gemma 2 2B IT int8 | .task | ~1.8GB | 후보 (Kaggle 로그인 게이트 있음 — 수동 배치 필요) |
| Qwen2.5 1.5B | 양자화 | ~1.2GB | 후보 (한국어 품질 중심) |
| Llama 3.2 1B | 양자화 | ~0.8GB | 후보 |

대부분의 HuggingFace/Kaggle 모델은 라이선스 게이트(로그인) 때문에 앱 내 자동 다운로드가 불가능하여,
게이트 없는 Gemma 4 E2B (.litertlm)를 1차 실측 대상으로 삼는다. S22에서 품질·속도가 부족하면
더 작은 양자화 모델로 교체한다 (PRD 원칙: 기능 삭제 아님).

## 벤치마크 항목 (S22 실측)

1. 토큰/초 (생성 속도)
2. 첫 토큰까지 지연 (TTFT)
3. 메모리 사용량 (peak RSS)
4. 로딩 시간
5. 배터리/발열 (장시간 대화)

## 일정

- [x] PRD-02 엔진/추상화 구현
- [x] 런타임 1차 후보(LiteRT-LM) 앱 내 통합 — LiteRtModel, 폴백 EchoModel
- [x] 설정 화면 모델 자동 다운로드 (Gemma 4 E2B .litertlm)
- [ ] S22 실측 벤치마크 → 결과 본 문서에 기록
- [ ] 최종 모델 확정

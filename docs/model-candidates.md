# 로컬 모델 후보 및 벤치마크 계획 (PRD-02)

## 원칙

- 후보 모델은 S22에서 성능·메모리·품질을 실측한 후 확정한다. (PRD-00: 문서만 보고 최종 모델을 고정하지 않는다)
- 개발용 클라우드 모델은 최종 앱의 필수 의존성이 아니다.
- `EchoModel`은 모델 미배치 시의 개발용 폴백이다.

## 교체 지점

```kotlin
// OnDeviceAiApplication.kt (ServiceLocator)
// files/models/에 모델이 있으면 MediaPipeModel, 없으면 EchoModel
val chatModel: ChatModel = modelCatalog.findModelFile()
    ?.let { MediaPipeModel(application, it.absolutePath) }
    ?: EchoModel()
```

UI·엔진 코드 변경 없이 구현체만 교체하면 된다.

## 런타임

| 런타임 | 특징 | 상태 |
|---|---|---|
| MediaPipe LLM Inference (`tasks-genai` 0.10.35) | Google 공식, `.task`/`.bin`/`.gguf` 지원, 세션 스트리밍 | **앱 내 통합 완료** (1차 후보, `MediaPipeModel`) |
| llama.cpp (JNI) | 광범위한 모델 지원, 양자화 | 예비 (빌드 복잡도 높음) |
| MLC LLM | GPU 가속(Vulkan), 컴파일 기반 | 예비 (모델별 사전 컴파일 필요) |

## 후보 모델 (경량 양자화, 추정치 — 실측 후 확정)

| 모델 | 파라미터 | 양자화 | 예상 메모리 | 상태 |
|---|---|---|---|---|
| Gemma 2 2B IT | 2B | int8 (.task) | ~1.8GB | 1차 실측 대상 |
| Qwen2.5 1.5B | 1.5B | Q4 | ~1.2GB | 후보 (한국어 품질 중심) |
| Phi-3.5 mini | 3.8B | Q4 | ~2.5GB | 후보 |
| Llama 3.2 1B | 1B | Q4 | ~0.8GB | 후보 |

한국어 품질을 고려해 Qwen2.5 계열과 Gemma 계열을 우선 실측한다. 모델 배치 방법은 docs/model-setup.md.

## 벤치마크 항목 (S22 실측)

1. 토큰/초 (생성 속도)
2. 첫 토큰까지 지연 (TTFT)
3. 메모리 사용량 (peak RSS)
4. 로딩 시간
5. 배터리/발열 (장시간 대화)

## 일정

- [x] PRD-02 엔진/추상화 구현
- [x] 런타임 1차 후보(MediaPipe LLM Inference) 앱 내 통합 — `MediaPipeModel`, 폴백 `EchoModel`
- [ ] S22 실측 벤치마크 → 결과 본 문서에 기록
- [ ] 최종 모델 확정

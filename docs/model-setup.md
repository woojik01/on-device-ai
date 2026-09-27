# 로컬 모델 배치 가이드 (S22 실기 테스트용)

모델 바이너리(수백 MB~수 GB)는 Git에 커밋하지 않는다. adb로 기기 내부 저장소에 직접 배치한다.

## 1. 모델 파일 준비

MediaPipe LLM Inference(`com.google.mediapipe:tasks-genai`)가 지원하는 형식: `.task`, `.bin`, `.gguf`

1차 실측 후보 (docs/model-candidates.md 참조):
- Gemma 2 2B IT int8 (`.task`) — Google AI Edge 배포본
- Qwen2.5 1.5B / Llama 3.2 1B (양자화 GGUF)

## 2. 기기에 배치 (debug 빌드, run-as 사용)

앱 재설치 시 데이터가 유지되는 `files/models/` 디렉터리를 사용한다.

```bash
adb push gemma-2b-it-int8.task /data/local/tmp/model.task
adb shell "run-as com.woojik.ondeviceai mkdir -p files/models"
adb shell "run-as com.woojik.ondeviceai sh -c 'cat /data/local/tmp/model.task > files/models/model.task'"
adb shell "rm /data/local/tmp/model.task"
```

> 여러 모델을 넣어두면 앱은 가장 최근에 추가된 파일을 사용한다.

## 3. 동작 확인

- 모델 파일이 있으면: `MediaPipeModel`로 로컬 추론 (오프라인 동작)
- 없으면: 개발용 `EchoModel`로 폴백 (모델이 없다는 안내 없이 에코 응답)

## 4. 벤치마크 기록

S22 실측 결과(토큰/초, TTFT, 메모리, 로딩 시간)는 docs/model-candidates.md에 기록한다.

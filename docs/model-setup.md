# 로컬 모델 배치 가이드 (S22 실기 테스트용)

모델 바이너리(수백 MB~수 GB)는 Git에 커밋하지 않는다. **기기에서 앱을 통해 직접 가져온다** (PC/adb 불필요).

## 1. 모델 파일 다운로드 (기기 브라우저)

MediaPipe LLM Inference가 지원하는 확장자: `.task` / `.bin` / `.gguf`

1차 실측 후보 (docs/model-candidates.md 참조):
- **Gemma 2 2B IT int8 (`.task`)** — Google AI Edge 배포본 (Kaggle 로그인 후 다운로드 가능)
- Qwen2.5 1.5B / Llama 3.2 1B (양자화) — `.gguf`는 MediaPipe 버전에 따라 지원이 다를 수 있으므로 `.task`를 우선한다

## 2. 앱에서 가져오기

1. 앱 → **설정 → 로컬 모델 → "모델 파일 가져오기"**
2. 다운로드한 모델 파일 선택
3. 복사 완료까지 대기 (파일이 커서 수 분 걸릴 수 있음)
4. 표시가 `사용 중: mediapipe-...`로 바뀌면 완료

가져오기 실패 시 안내:
- "지원하지 않는 파일 형식" → 확장자가 .task/.bin/.gguf가 아님
- "복사하지 못했어요" → 저장 공간 부족 가능성 (모델 크기의 2배 이상 여유 필요)

## 3. 동작 확인

- 모델 파일이 있으면: `MediaPipeModel`로 로컬 추론 (오프라인 동작)
- 없으면: 개발용 `EchoModel` 폴백 — 설정 화면에 "로컬 모델 없음" 표시

## 4. 벤치마크 기록

S22 실측 결과(토큰/초, TTFT, 메모리, 로딩 시간)는 docs/model-candidates.md에 기록한다.

## 부록: adb로 배치하는 방법 (PC 사용 가능 시)

```bash
adb push gemma-2b-it-int8.task /data/local/tmp/model.task
adb shell "run-as com.woojik.ondeviceai mkdir -p files/models"
adb shell "run-as com.woojik.ondeviceai sh -c 'cat /data/local/tmp/model.task > files/models/model.task'"
adb shell "rm /data/local/tmp/model.task"
```

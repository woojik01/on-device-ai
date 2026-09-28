# 로컬 모델 배치 가이드 (S22 실기 테스트용)

모델 바이너리(수백 MB~수 GB)는 Git에 커밋하지 않는다. **기기에서 앱을 통해 직접 배치한다** (PC/adb 불필요).

## 1. 앱 내 자동 다운로드 (권장)

1. 앱 → **설정 → 로컬 모델 → "모델 자동 다운로드 (Gemma 4 E2B, 약 2.6GB)"**
2. Wi-Fi 연결 상태에서 대기 (수 분~수십 분, 진행률 표시 확인)
3. 표시가 "사용 중: litert-gemma-4-E2B-it"로 바뀌면 완료

다운로드는 임시 파일로 진행되므로 중간에 실패/취소해도 기존 모델을 덮어쓰지 않는다.
실패 시 안내:
- "네트워크 연결을 확인해 주세요" → Wi-Fi 재연결 후 재시도
- "저장 공간이 충분한지 확인해 주세요" → 약 3GB 이상 여유 필요

기본 다운로드 대상: Gemma 4 E2B IT (.litertlm, LiteRT-LM 전용 형식, 약 2.6GB)
— 후보 선정 이유와 벤치마크 계획은 docs/model-candidates.md.

## 2. 파일로 직접 가져오기

이미 기기에 모델 파일이 있는 경우:

1. 앱 → **설정 → 로컬 모델 → "파일로 직접 가져오기"**
2. 모델 파일 선택 (.litertlm / .task / .bin / .gguf)
3. 복사 완료까지 대기 (파일이 커서 수 분 걸릴 수 있음)

## 3. 동작 확인

- .litertlm → LiteRT-LM (LiteRtModel) 로컬 추론 (오프라인 동작)
- .task / .bin / .gguf → MediaPipe LLM Inference (MediaPipeModel)
- 없으면: 개발용 EchoModel 폴백 — 설정 화면에 "로컬 모델 없음" 표시

모델이 여러 개면 가장 최근 파일을 사용한다.

## 4. 벤치마크 기록

S22 실측 결과(토큰/초, TTFT, 메모리, 로딩 시간)는 docs/model-candidates.md에 기록한다.

## 부록: adb로 배치하는 방법 (PC 사용 가능 시)

```bash
adb push gemma-4-E2B-it.litertlm /data/local/tmp/model.litertlm
adb shell "run-as com.woojik.ondeviceai mkdir -p files/models"
adb shell "run-as com.woojik.ondeviceai sh -c 'cat /data/local/tmp/model.litertlm > files/models/model.litertlm'"
adb shell "rm /data/local/tmp/model.litertlm"
```

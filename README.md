# On-Device AI Girlfriend

휴대폰에서 동작하는 개인용 온디바이스 AI 캐릭터 프로젝트.

## 개발 원칙

- Android 우선
- AI 추론은 가능한 한 온디바이스
- 사용자 대화와 기억은 기본적으로 기기 내부에 저장
- 기능은 단계별 PRD를 완료하면서 확장
- 각 단계는 실제 기기 테스트를 통과해야 완료
- 명시하지 않은 기능은 임의로 추가하지 않음
- 기존 기능을 수정할 때 회귀(regression)를 방지
- 개발 과정에서 네트워크 의존성을 최소화하고, 온디바이스 AI 경로를 최종 목표로 유지

## PRD 진행 순서

1. [PRD-00 프로젝트 기준](docs/prd/PRD-00-project-baseline.md)
2. [PRD-01 Android 앱 기반](docs/prd/PRD-01-android-foundation.md)
3. [PRD-02 대화 엔진](docs/prd/PRD-02-conversation-engine.md)
4. [PRD-03 캐릭터 시스템](docs/prd/PRD-03-character-system.md)
5. [PRD-04 기억 시스템](docs/prd/PRD-04-memory-system.md)
6. [PRD-05 감정·관계 시스템](docs/prd/PRD-05-emotion-relationship.md)
7. [PRD-06 음성 시스템](docs/prd/PRD-06-voice.md)
8. [PRD-07 온디바이스 최적화 및 출시](docs/prd/PRD-07-on-device-release.md)

## 완료 기준

최종 v1.0은 인터넷 연결 없이 텍스트 대화가 가능하고, 캐릭터의 기억·상태·관계가 앱 재실행 후에도 유지되며, 실제 Android 기기에서 안정적으로 동작해야 한다.

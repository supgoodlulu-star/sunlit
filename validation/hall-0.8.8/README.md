# 명예의 전당 검증

실제 Forge 1.20.1 클라이언트, 1280×900, 격리된 SunlitHallTest 월드.
화면의 순위·이름·점수는 QA가 만든 예시이며 운영 서버 기록이 아닙니다. 이미지 생성이나 합성 화면이 아닙니다.

- screenshots/world-podium-example-records.png: 실제 월드의 3인 전시대.
- screenshots/monthly-leaderboard-example-records.png: 명령어로 연 전체 순위.
- screenshots/live-rank-change-example-records.png: 서버 점수 변경 후 실제 패킷으로 갱신된 순위.
- screenshots/small-gui-example-records.png: GUI 배율 4의 화면.
- screenshots/profile-hall-entry.png: 프로필 진입 버튼.

QA는 배포 JAR에 포함하지 않습니다. 실행: `./gradlew -PintegrationMods=true -PintegrationModDir=/workspace/reference/mods -I tools/visual-preview/hall-check.gradle runClient`. QA 월드에 예시 기록과 블록을 만들기 때문에 **테스트용 월드에서만** 실행해야 합니다.

## 결과

- 기존 수첩/아이템 자랑 + 명예의 전당 **44 GameTests 모두 통과**. Java 17 / Forge 47.3.0 / Skills 0.16.1 / ServerNick / Patchouli 개발용 연동 환경.
- 한국 시간 월말 경계, 재시작/과거 이름 보관, 시간 역행, 동점, 페이지 경계, XP 최초 기준값·창작 모드 제외·오프라인 월 건너뛰기, 업무 중복 완료 방지, 축제 남은 목표까지만 집계, 운영자 설치 권한/엔티티 저장·보호 검증.
- 실제 클라이언트: 공개 명령어, 3개 전시 인형, 서버 기록 변화에 따른 순위와 전시 인물 변경, 인형 우클릭, 작은 GUI 배율, 화면 닫기, 프로필 진입 버튼 통과 (`HALL_CLIENT_PASSED`).
- `clean runGameTestServer build` 성공. 전체 선릿밸리 모드팩/실제 운영 서버 검증은 별도이며, 모든 스킨 다운로드 환경을 보장하는 검사는 아닙니다.
- 개발 환경의 인증 키/OpenAL 장치 연결 경고는 QA 통과 여부와 구분됩니다.
- 소스 ZIP 별도 폴더 독립 빌드 성공. 생성된 JAR의 모든 ZIP 엔트리 내용이 배포 JAR와 동일하며 SHA-256 검증 완료.

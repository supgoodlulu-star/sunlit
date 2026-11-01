# 낚시 판정 막대 +40% 실제 게임 미리보기

실제 Stardew Fishing 3.7의 서버 이벤트 → S2CStartMinigamePacket → FishingScreen 경로로 촬영했습니다. 기본 BAR_SIZE=36을 읽고 테스트 이벤트에만 Math.round(36×1.4)=50을 적용했습니다. 네이티브 클라이언트 minigame.getBarSize() 값도 36 / 50인지 확인했습니다.

- screenshots/fishing-bar-default.png: 기본 36 게임 픽셀
- screenshots/fishing-bar-plus40.png: +40% 요청, 정수 반올림 50 게임 픽셀

두 장면은 같은 물고기와 행동 매개변수, GUI 배율로 비교합니다. 비교를 위해 클라이언트의 물고기·판정 막대 위치 및 진행률을 고정하고 안내 팝업·HUD를 숨겼습니다. 캡처의 막대는 새로 그린 시안이 아니라 실제 모드의 낚시 화면입니다. UI 배율에 따라 화면상 픽셀 수는 달라집니다. 실제 플레이에서는 스킬·장비 등으로 기본 막대 길이가 달라질 수 있습니다.

전체 사용자 모드팩의 플레이 기록이 아닌 별도 테스트 월드 및 실제 관련 모드 조합의 비교 장면입니다. 게임용 기본 크기나 잠재옵션·확률·보상·저장 데이터는 변경하지 않았습니다. 이번 결과물은 +40% 크기 확인용이며 새 배포 JAR는 없습니다.

미리보기 코드는 addons/reforged/qa/FishingBarPreview.java와 bar-preview.gradle입니다. Java 17, 실제 4.1.5A 관련 모드 JAR를 준비한 상태에서 addon gradlew에 -I qa/bar-preview.gradle runClient 및 기존 integration 속성들을 적용해 실행합니다. QA 소스는 일반 배포 JAR에 포함되지 않습니다.

희귀 물고기·보물상자 옵션 분리 및 낚시 막대 크기 옵션 추가는 별도 구현 단계이며, 등급별 막대 증가량은 아직 확정하지 않았습니다.

# 실제 자동낚시 검증

Minecraft 1.20.1 / Forge 47.4.0 / Java 17 / Stardew Fishing 3.7 / Aquaculture 2.5.7 / Nether Depths Upgrade 3.1.5 / Sunlit Reforged dev.8 / Quality Food 2.4.3 및 기존 낚시·판매함 테스트 의존성을 사용했습니다. 외부 JAR는 배포하지 않습니다. 파일 SHA256은 REFERENCE_MODS_SHA256.txt 참조.

스크린샷은 실제 Minecraft 테스트 월드에서 캡처한 것이며 생성 시안이 아닙니다. 적대적 몹이 방해하지 않는 평화로운 난이도의 별도 평지 월드를 사용했습니다. 실제 투척/입질/보상/내구도는 원래 코드를 사용합니다.

QA 소스에서만 상자 출현 플래그를 강제로 켜서 수집을 검사했습니다. 성공 여부·진행도·상자 획득 상태·보상은 강제하지 않았습니다. 테스트용 리스너와 입력 테스트 클래스는 배포 JAR에 포함되지 않습니다. 실제 게임의 상자 확률은 기존 설정입니다.

네이티브 입력 테스트는 실제 FishingScreen/FishingMinigame과 기본 4개 어종 행동(대구·연어·피라미·참치), 막대 36/72픽셀, 이동 패턴 seed를 사용합니다. 테스트 화면은 전역 화면으로 열지 않고 자체 tick을 진행하여 가짜 완료 패킷이나 보상을 만들지 않습니다. 테스트 성적은 모든 어종/설정의 성공률을 보장하지 않습니다.

실제 플레이 경로는 F8 이벤트 → 정상 Item.use/서버 찌 생성 → 실제 입질 → 정상 회수 → 실제 미니게임 자동 입력 → 원래 완료 패킷 → 서버 보상/인벤토리 수령 → 재투척까지 검사했습니다. 일반 낚싯대와 넵튜늄 낚싯대를 각각 사용했습니다. 넵튜늄에는 기존 잠재옵션 3줄과 외부 NBT를 설정하고 유지 여부를 검사했습니다.

최종 요청에 따라 기본 인벤토리가 가득 차도 계속 낚시합니다. QA에서 36칸을 모두 채운 뒤 실제 포획과 재투척, 같은 상태 유지, 새 어획물의 바닥 드롭을 확인했습니다. 기본 인벤토리가 가득 찼다는 이유로 중지하는 코드는 배포하지 않습니다.

재현 전 동일 모드 조합으로 ‘SunlitAutoFishingTest’라는 평지 테스트 월드를 한 번 생성하고 종료하세요. QA는 테스트 구역을 바꾸므로 실제 플레이 월드 이름을 사용하지 않습니다.

명령: ./gradlew -PforgeVersion=47.4.0 -PintegrationMods=true -PintegrationModDir=/path/to/mods -I tools/fish-sizes/integration.gradle -I tools/auto-fishing/client.gradle runGameTestServer runClient build

기존 판매함 fixture는 물고기 크기/품질/판매 계산의 회귀 검사입니다. 전체 사용자 모드팩과 Essential 원격 2인 접속, 용암 낚시의 실제 전체 사이클은 이번 검증 범위 밖입니다.

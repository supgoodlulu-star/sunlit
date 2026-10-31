# 꿈방울 적용 검증

Forge 47.4.0 / Minecraft 1.20.1 / Java 17. 실제 관련 연동 모드 조합으로 검사했습니다.

서버 GameTest 24/24 통과. 실제 클라이언트 QA REFORGED_CLIENT_PASSED. 기존 저장 아이템 3종의 ID·수량·임의 NBT 복원, 기존 잠재능력·승급·기존 줄 선택·대기 복원·은행 구매·작은 GUI 클릭을 확인했습니다. 종류별 이름·툴팁·텍스처 및 별도 입체 모델도 확인했습니다.

UNCHANGED_LOGIC.txt는 이전 커밋과 동일한 핵심 로직의 SHA-256입니다. ASSET_CHECK.json은 PNG alpha·좌표·UV·프로젝트 내장 텍스처 구조 검사 결과입니다.

screenshots는 실제 Minecraft 캡처입니다. dreamdrop-model-gallery의 위쪽은 인벤토리 아이콘, 아래쪽은 게임 아이템 렌더러가 그린 입체 모델입니다. dreamdrop-*-held는 실제 손에 든 상태입니다. 시안 자체나 오프라인 3D 렌더를 게임 적용 결과로 표시하지 않습니다.

전체 모드팩 및 Essential 2인 접속을 새로 검증한 것은 아닙니다. 원본 Reforged 접두사 연동의 기존 미완료 상태는 유지됩니다.

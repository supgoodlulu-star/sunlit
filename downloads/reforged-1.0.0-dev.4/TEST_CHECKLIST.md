# 호환성 및 렌더링 검증

- 서버 GameTest 24개: 기존 잠재능력·확률·선택 복원·은행·보상·생활 보너스 테스트와 이전 아이템 ID/수량/임의 NBT 복원 테스트.
- 실제 Minecraft QA: 기존 아이템의 한국어 이름, 3개 상품·가격, 구매/중복 요청·마스터 선택·잠재능력 툴팁·작은 GUI 클릭.
- 실제 모델 로딩: 인벤토리/손 모델의 누락 텍스처 여부, 입체 모델 면 수, 종류별 아이템 툴팁 검사.
- 실제 아이템 렌더러의 모델 비교 화면과 손에 든 상태 캡처. 캡처는 validation/reforged-dreamdrops/screenshots를 참조합니다.
- Potential.java, Rules.java, Economy.java, StationMenu.java, Ledger.java, Effects.java, Skills.java 및 기본 확률 JSON은 이전 커밋과 SHA-256 비교합니다.
- PNG alpha·JSON 모델 참조·좌표·UV와 BB 프로젝트 내장 텍스처를 점검합니다.

전체 모드팩 및 두 사용자의 실제 네트워크 접속은 이번 검증 범위에 포함하지 않습니다.

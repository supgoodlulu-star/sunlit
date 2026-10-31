# Sunlit Reforged 1.0.0-dev.3 설치

이 배포는 통합 검증 중인 개발판입니다. Reforged 원본 JAR 미확보로 접두사 연동은 완료되지 않았습니다. 기존 월드 백업 후 별도 테스트 서버에서 사용하세요.

Java 17 / Minecraft 1.20.1 / Forge 47.4.0 기준입니다. 서버와 접속 클라이언트 양쪽 mods에 sunlit-reforged-1.0.0-dev.3.jar를 설치합니다. Journal 보상 연동은 기존 Journal JAR 하나를 sunlit-journal-core-0.8.11-reforged-bridge.jar로 교체합니다. Journal 중복 설치는 금지합니다.

실제 연동 확인 버전: Create 6.0.8, Create Numismatics 1.1.0, Pufferfish Skills 0.16.1, Stardew Fishing 3.7, Serene Seasons 9.1.0.2. 외부 모드는 이 배포에 포함하지 않습니다.

/재련 명령어로 상점·잠재능력 창을 엽니다. 마스터 큐브 선택 대기 중인 장비는 서버 보관 상태이므로 창을 다시 열어 선택을 마치세요. 애드온 제거 전 대기 선택과 보관 슬롯을 모두 비우세요.

은행 잔액은 Numismatics의 실제 계좌를 사용합니다. 계좌가 없거나 잔액이 부족하면 구매하지 않습니다.

서버 데이터팩으로 potential_rules/default.json과 fishing_pools 아래 JSON을 덮어쓸 수 있습니다. 기본값은 애드온 소스 resources/data/sunlit_reforged에 있습니다. 잘못된 설정은 로그에서 확인하세요.

소스 빌드: addons/reforged 디렉터리에서 ./gradlew build (Java 17, 최초 실행은 Maven 네트워크 필요). Journal 동반 버전은 저장소 루트에서 ./gradlew build. source ZIP에는 외부 모드와 Gradle 캐시를 포함하지 않습니다.

## 1.0.0-dev.3 변경
사용자 요청으로 재련 망치를 제거했습니다. 상점 상품·구매 처리·설정에서 제외하고 큐브 3종만 판매합니다. 빈 자리는 사용 가능한 장비 안내로 바꿨습니다. 이전 데이터팩의 가격 4개 형식은 앞의 큐브 가격 3개만 읽으며, 이전 망치 설정은 무시합니다. 삭제한 상품 번호의 구매 요청은 차감 없이 거절합니다. 기존 장비 NBT와 큐브 기능은 유지합니다.

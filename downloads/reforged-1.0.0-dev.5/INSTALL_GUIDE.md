# 잠재능력 배율 조정판 1.0.0-dev.5

순서는 레어 → 에픽 → 유니크 → 레전드리입니다. 요청한 10개 옵션의 기본 수치만 바꿨습니다. 나머지 9개 옵션, 가중치, 승급/줄 등급 확률, 가격, 보상과 기존 장비 데이터는 유지합니다.

## 방법 1 — 새 JAR
서버와 클라이언트의 기존 sunlit-reforged JAR 하나를 sunlit-reforged-1.0.0-dev.5.jar로 교체합니다. 수첩 JAR는 기존 0.8.12를 그대로 사용하세요.

## 방법 2 — 데이터팩만 적용
기존 꿈방울 JAR에서도 새 설정을 사용할 수 있습니다. 서버 월드/datapacks에 sunlit_balance.zip을 그대로 넣고 /reload를 실행하세요. /datapack list enabled에서 활성화 여부를 확인합니다. ZIP 안의 pack.mcmeta와 data가 바로 최상위에 있어야 합니다.

폴더로 사용하려면 sunlit_balance 폴더를 만든 뒤 ZIP 내용을 풀어 넣으세요. 폴더와 ZIP을 동시에 설치하지 않습니다. 직접 편집할 파일은 data/sunlit_reforged/potential_rules/default.json입니다. 함께 제공한 default.json은 완전한 설정 파일입니다.

이미 같은 경로를 덮어쓰는 데이터팩이 있다면 새 JAR보다 해당 데이터팩이 우선하므로 기존 default.json도 이번 파일로 교체해야 합니다. 여러 팩이 같은 설정을 덮어쓰지 않도록 사용 중인 설정 팩을 확인하세요.

## 적용 범위
새로 뽑는 옵션부터 적용합니다. 기존 장비의 수치와 이미 생성된 찬란한 꿈방울 선택 대기 결과는 바꾸지 않습니다. 예전 줄을 유지하면 예전 수치가 그대로 남습니다.

Minecraft 1.20.1 / Java 17 / Forge용입니다. 소스 빌드는 addons/reforged에서 ./gradlew build입니다.

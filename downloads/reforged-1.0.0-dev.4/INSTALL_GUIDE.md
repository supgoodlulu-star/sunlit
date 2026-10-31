# 꿈방울 적용판 1.0.0-dev.4

Minecraft 1.20.1 / Java 17 / Forge 47.4.0에서 검증했습니다. 서버와 클라이언트 모두 기존 sunlit-reforged JAR 하나를 제거하고 sunlit-reforged-1.0.0-dev.4.jar로 교체합니다. 중복 설치하지 마세요. 별도 리소스팩은 필요하지 않습니다.

기존 Journal 0.8.11-reforged-bridge는 그대로 사용합니다. 이 작업으로 수첩 JAR를 다시 교체할 필요는 없습니다. /재련 명령어와 꿈방울 우클릭으로 기존 공방을 엽니다.

레지스트리 ID suspicious_cube / artisan_cube / master_cube는 유지합니다. 기존 보유 아이템·상점·보상·드롭·NBT는 별도 변환 없이 동일한 아이템으로 연결됩니다. 직접 이름을 붙인 아이템은 사용자 지정 이름을 유지합니다.

작은 꿈방울 500 Sun / 영롱한 꿈방울 2,500 Sun / 찬란한 꿈방울 10,000 Sun. 망치는 제거된 상태를 유지합니다.

소스 빌드: Java 17에서 addons/reforged/gradlew -p addons/reforged build. 첫 빌드는 Maven 접속이 필요합니다. Blockbench 프로젝트는 Dreamdrop_Assets.zip의 blockbench에 있습니다. 텍스처가 프로젝트에 포함되어 있어 단독으로 열 수 있습니다.

기존 개발판의 Reforged 접두사 연동 및 전체 모드팩 검증 미완료 상태는 이번 외형 교체로 해결되는 항목이 아닙니다.

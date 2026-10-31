# Sunlit Reforged

Minecraft 1.20.1 / Forge 47.4.0 / Java 17. 개발판 1.0.0-dev.5.

빌드: `./gradlew build`

게임 테스트: `./gradlew runGameTestServer`. 실제 모드 연동 검증은 build.gradle의 integrationMods / economyIntegration / journalIntegration / fishingIntegration 및 integrationModDir 속성을 사용합니다. 외부 모드는 포함하지 않습니다. 테스트 소스와 qa는 배포 JAR에 포함하지 않습니다.

설치·검증 범위·미완료 항목은 저장소 downloads/reforged-1.0.0-dev.5 문서를 참조하세요. Reforged 원본 JAR를 확인하지 못했으므로 접두사 연동 완료판이 아닙니다.

상점은 꿈방울 3종만 판매합니다. 재련 망치는 사용자 요청으로 제거했습니다.

꿈방울 표시 이름·아이콘·입체 모델만 교체하며 레지스트리 ID와 잠재능력 규칙은 유지합니다. art/blockbench에 편집 프로젝트가 있습니다. tools/build_dreamdrop_models.py로 입체 모델과 UV 아틀라스를 재생성할 수 있습니다(Pillow와 ImageMagick 필요).

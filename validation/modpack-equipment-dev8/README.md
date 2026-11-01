# 선릿밸리 장비 지원 검증

기준 모드팩: Society Sunlit Valley 4.1.5A. 실제 모드 버전과 SHA256은 REFERENCE_MODS_SHA256.txt 참조. Oreganized는 Gradle 공백 경로 호환을 위해 동일 바이트의 Oreganized-4.3.2.jar 별칭을 사용했습니다.

qa/modpack-kubejs/startup_scripts/sunlit-reforged-qa/registerWeapons.js는 모드팩 overrides/kubejs/startup_scripts/registration/registerWeapons.js에서 추출한 등록 코드입니다(줄바꿈/행 끝 공백 정리). 이를 실제 KubeJS에서 실행하여 갤럭시 검 등 원래 도구 클래스·등급으로 검증합니다. 전체 모드팩 스크립트를 실행하거나 임의 가짜 아이템으로 대체한 검증이 아닙니다. 외부 JAR는 동봉하지 않습니다.

스크린샷은 실제 Minecraft 클라이언트의 공방 UI에서 촬영합니다. 생성 이미지가 아닙니다. 버튼 → 서버 패킷 → 잠재능력 갱신을 확인합니다. 관련 모드 조합의 테스트 월드이며 사용자 전체 모드팩 실전 플레이 또는 Essential 멀티 검증은 아닙니다.

재현: addons/reforged/gradlew -p addons/reforged -PintegrationMods=true -PeconomyIntegration=true -PjournalIntegration=true -PfishingIntegration=true -PintegrationModDir=/path/to/mods -I qa/modpack-check.gradle runGameTestServer build

클라이언트: 위 설정에 -I qa/client-check.gradle -PpreviewOutput=/path/to/output 를 추가하고 runClient 실행.

갤럭시 검 렌더링 검증은 society-1.22.jar에서 원래 세 무기 텍스처를 QA 런타임 리소스로만 추출합니다. 외부 텍스처 파일은 소스/배포 JAR에 포함하지 않습니다. 검증 스크린샷의 갤럭시 검은 식별을 위해 테스트 월드에서 한국어 이름을 붙였습니다.

goldHarvestLevel.js는 원본 registration/modifyItems.js의 금 도구·금 망치 채굴 등급 상향 부분만 분리한 QA 스크립트입니다. 금 망치를 포함해 재련 제외인지 검사하고, 관측한 두 도구의 실제 등급을 로그에 기록합니다. 테스트 환경에서 해당 수정이 전체 모드팩과 동일한 효과를 냈다고 가정하지 않습니다.

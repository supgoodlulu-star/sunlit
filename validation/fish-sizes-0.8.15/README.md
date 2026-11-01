# 실제 게임 및 원본 판매함 계산 검증

Society Sunlit Valley 4.1.5A / Forge 47.4.0 / Java 17 기준. 실제 관련 모드와 원본 globalShippingBinHandlers.js를 실행했습니다. fixture 원본 경로는 모드팩 overrides/kubejs/startup_scripts/globalShippingBinHandlers.js이며 테스트 소스에는 줄바꿈/행 끝 공백만 정리했습니다. 이 원본 파일을 사용자 모드팩에 덮어쓰지 않습니다. 설치 대상은 별도 sunlit_fish_size_shipping.js입니다.

서버 GameTest 61개 및 실제 클라이언트 검사 통과. 스크린샷은 생성 이미지가 아닌 실제 게임 캡처입니다. 테스트 월드에서 5개 크기를 검증하기 위해 서버 생성한 물고기입니다. 전체 모드팩의 실제 판매함 블록/코인 출력이나 Essential 멀티 접속을 검증한 것은 아닙니다.

재현: ./gradlew -PforgeVersion=47.4.0 -PintegrationMods=true -PintegrationModDir=/path/to/mods -I tools/fish-sizes/integration.gradle runGameTestServer build
클라이언트: 위 설정에 -I tools/fish-sizes/client.gradle 추가 후 runClient 실행.

외부 JAR는 배포하지 않습니다. 같은 버전의 파일을 준비해야 하며 참조 SHA256은 별도 파일에 기록했습니다. QA/테스트 코드는 배포 JAR에 포함되지 않습니다.

# 실제 채팅 검증

Minecraft 1.20.1 / Forge 47.3.0 / Java 17과 실제 Sunlit Reforged 1.0.0-dev.8, ServerNick, Pufferfish Skills, Patchouli로 실행했습니다. 스크린샷은 실제 테스트 월드에서 촬영한 것이며 생성 시안이 아닙니다. 등급별 장비는 검증을 위해 서버에서 직접 설정한 테스트 아이템이며 무작위 강화 결과나 실제 플레이어 소유 장비가 아닙니다.

/자랑하기, 일반 채팅 토큰, 실제 SHOW_ITEM 호버, 닉네임·칭호, 중복 전송 방지, 빈손·취소 채팅·일반 채팅, 네 등급 색상과 원본 이름/NBT 보존을 검증했습니다. four-grade-showcase-chat.png에서는 원본 분홍색 이름이 호버에 남고 채팅은 각 잠재등급 색상으로 표시됩니다.

전체 사용자 모드팩 또는 Essential 원격 2인 접속 검증은 아닙니다. GUI 배율 2에서 캡처했습니다.

재현: ./gradlew -PintegrationMods=true -PintegrationModDir=/path/to/mods -I tools/visual-preview/showcase-check.gradle runClient
외부 모드 JAR는 포함하지 않습니다. 테스트 및 QA 클래스는 배포 JAR에 포함되지 않습니다.

# 수첩 0.8.7 — 아이템 자랑

펫 기능을 넣기 전 수첩 브랜치 `feature/journal-only-0.8.0`의 0.8.6(568a55d)을 기준으로 만들었습니다. 펫 작업은 `art/baby-pets-0.9.4`에 별도로 보존했고, 이 배포본에는 펫 기능을 포함하지 않습니다.

## 사용법

메인 손에 아이템을 들고 `/자랑하기`를 입력하면 접속 중인 플레이어 전체에게 두 줄을 보냅니다.

```text
✿ 룰루님이 아이템을 자랑합니다!
✦ [다이아몬드 곡괭이]
```

일반 채팅에서는 `[아이템]`이 같은 아이템 표시로 바뀝니다.

```text
입력: 쨔안~ [아이템]
표시: 예서: 쨔안~ [다이아몬드 곡괭이]
```

- 아이템 이름에 마우스를 올리면 Minecraft의 실제 아이템 툴팁을 표시합니다. 커스텀 이름·인챈트·설명·손상 등 NBT를 보존합니다. 아이템 ID/내구도 등 추가 정보 표시는 클라이언트의 고급 툴팁 설정을 따릅니다.
- 여러 개 묶인 아이템은 이름 뒤에 `× 수량`을 표시합니다.
- 닉네임 연동과 기존 대표 칭호의 색상·채팅 이미지를 유지합니다. 같은 메시지는 한 번만 방송합니다.
- 빈손이면 본인에게 안내하며 자랑/아이템 포함 채팅은 방송하지 않습니다. 보조 손만 아이템을 든 경우도 메인 손이 비어 있으면 안내합니다.
- 일반 플레이어도 사용할 수 있습니다. 아이템을 소비·복제·이동하지 않습니다. 클릭 명령이나 거래 기능을 추가하지 않습니다.
- 채팅에 보낸 시점의 아이템 복사본을 사용하므로 이후 손의 아이템을 교체하거나 이름을 바꿔도 이전 툴팁은 바뀌지 않습니다.

## 구현과 호환

`ItemShowcase`가 명령과 `SHOW_ITEM` 컴포넌트를 구성합니다. `[아이템]` 치환은 `JournalChat`의 기존 서버 스레드 처리 안에서 수행합니다. 텍스트의 원래 서식, 반복 토큰, 여러 컴포넌트에 나뉜 토큰을 처리합니다. 기존에 취소된 채팅은 재방송하지 않습니다. 토큰과 대표 칭호가 없는 일반 채팅은 기존 핸들러에 맡깁니다.

서버의 실제 메인 손 스택을 복사하고 바닐라 `ItemStack.getDisplayName()`을 사용합니다. 별도 아이템 정보 전송 패킷이나 클라이언트 전용 툴팁을 추가하지 않습니다. 수첩 데이터·업무 보상·칭호 저장 형식과 `journal-10` 프로토콜도 그대로입니다.

## 검증

- Pufferfish Skills / ServerNick / Patchouli 개발 연동 환경의 서버 GameTest 37개 통과(아이템 자랑 신규 4개 포함).
- 이름·인챈트·설명·손상·수량의 네트워크 직렬화 및 전송 후 원본 아이템 변경 검증.
- 실제 클라이언트의 명령/채팅 패킷으로 닉네임·칭호 이미지·중복 없는 방송·빈손 안내·취소된 채팅·일반 채팅 검증.
- 실제 채팅창에서 마우스를 올린 툴팁 캡처: [명령](validation/journal-0.8.7/screenshots/command-item-hover.png), [채팅 치환](validation/journal-0.8.7/screenshots/chat-placeholder-hover.png), [칭호와 커스텀 이름](validation/journal-0.8.7/screenshots/title-and-custom-item-hover.png).

위 이미지는 테스트 월드에서 찍은 실제 Minecraft 화면입니다. 디자인 시안이나 생성 이미지가 아닙니다. 클라이언트 확인 도구는 배포 JAR에 포함하지 않습니다.

```sh
./gradlew build
./gradlew -PintegrationMods=true -PintegrationModDir=/path/to/mods runGameTestServer
./gradlew -PintegrationMods=true -PintegrationModDir=/path/to/mods -I tools/visual-preview/showcase-check.gradle runClient
```

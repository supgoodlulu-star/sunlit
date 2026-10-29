# 0.8.0-journal 현재 검사

현재 배포 검증은 `JOURNAL_VALIDATION.md`를 기준으로 합니다. 아래의 스킨/하늘 정원 검사 설명은 구버전 역사 자료이며, 삭제한 실행 클래스에 의존하는 도구는 현재 빌드에서 사용하지 않습니다.

`preview.gradle`은 `JournalVisualPreview.java`만, `input-check.gradle`은 `JournalInputChatCheck.java`만, `reward-check.gradle`은 `RewardFlowCheck.java`와 테스트용 보상 fixture만 컴파일합니다. 배포 JAR에는 이 도구가 포함되지 않습니다.
보상 검사는 복사한 전용 월드 `SunlitCitadelTest`, 입력 검사는 `SunlitInputTest`를 사용합니다. 실제 월드에는 실행하지 마세요. 첫 진입에서 실험적 설정 안내의 진행을 눌러야 할 수 있습니다.

# UI 렌더링 검사

실제 Minecraft 클라이언트에서 서버 접속 없이 예시 데이터를 표시하고 `validation/screenshots/`에 15개 화면을 저장합니다. 화면 크기는 1280×900 및 960×720, GUI 크기는 2/3입니다. 이 Java 파일은 기본 소스 세트에 포함되지 않으며 배포 JAR에도 들어가지 않습니다. 원본 모드팩 연동 검사가 아닙니다.

JDK 17, 작동하는 X11/OpenGL 디스플레이와 ForgeGradle 환경에서 프로젝트 루트에서 실행합니다.

```sh
./gradlew -I tools/visual-preview/preview.gradle runClient
```

최초 실행의 Minecraft 접근성 안내가 나오면 계속 버튼을 누르세요. 언어를 한국어로 바꾼 뒤 프로필·기술·업무·업적·필터·기술 펼치기·업무 하위 탭·계절 업무 유무·농사 문구를 촬영하고 종료합니다. 실제 기술 설정이 없는 예시이므로 해금 기술은 `이름 미등록 기술`로 표시됩니다. 서버 요청 버튼은 이 검사에서 누르지 않습니다.

## 실제 입력·채팅 왕복 검사

기존 게임 월드가 아닌 서버 GameTest가 생성한 전용 월드를 `run/saves/SunlitInputTest`에 복사한 뒤 아래를 실행합니다. 테스트 월드의 테스트 플레이어 닉네임과 칭호를 변경하므로 실제 플레이 월드를 사용하지 마세요.

```sh
./gradlew -PintegrationMods=true -PintegrationModDir=/path/to/provided-mods -I tools/visual-preview/input-check.gradle runClient
```

제공 모드 3개(Server Nick, Pufferfish Skills, Patchouli)가 필요합니다. 테스트는 웅크림/우클릭 입력 이벤트와 키 등록·재지정·키 입력을 실제 클라이언트에서 발생시키고 서버 프로필 응답을 확인합니다. 실제 채팅을 보내 수신 횟수와 칭호/닉네임/테마 색·변경/해제·선행 취소를 검사합니다. 테스트 중에만 키를 H로 지정합니다. 성공 시 `SUNLIT_INPUT_CHAT_CHECK PASS`를 로그에 기록하고 종료합니다. 실제 사람의 마우스 클릭이나 인터넷 멀티플레이 검사는 아닙니다.

## 하늘 정원 검사

`runGameTestServer`가 만든 테스트 월드를 `run/saves/SunlitCitadelTest`로 복사한 뒤 실행합니다. 실제 월드를 사용하지 마세요. 테스트는 그 월드의 플레이어 장비·게임 모드·해금 상태를 변경합니다.

```sh
./gradlew -PintegrationMods=true -PintegrationModDir=/path/to/provided-mods -I tools/visual-preview/sky-check.gradle runClient
```

Minecraft가 실험적 차원 안내를 표시하면 테스트 복사본에서 계속을 선택합니다. 나침반 use 입력 → 하늘 차원 → 실제 맵 촬영 → 기록/퍼즐 → 두 스킨 네트워크 적용과 실제 모델 검증 → 수첩 → 귀환을 검사합니다. 성공 표시는 `SKY_GARDEN_CHECK PASS`입니다. 해당 검사 클래스는 배포 JAR에 포함하지 않습니다.

0.5.1 검사에서는 신규 건축을 확인하기 위해 테스트 복사본의 확장판 생성 플래그를 초기화합니다. 촬영은 렌더 거리 20, 구름 끄기로 진행합니다. 기존 개인 진행도를 보존하는 실제 업데이트와 달리 테스트 플레이어의 퍼즐 상태를 초기화하므로 반드시 버려도 되는 복사본에서만 실행하세요.

0.5.2는 백색 성채 시안 차원의 실제 조형을 `citadel-overview.png`, `citadel-interior.png`로 촬영합니다. 시안의 모든 높은 탑 내부와 전체 도보 동선을 검사하는 테스트는 아닙니다.

## 장비 모델 검사

기존 테스트 복사 월드에서 `-I tools/visual-preview/skin-check.gradle runClient`를 실행하면 건축/퍼즐 상태를 초기화하지 않고 해금된 장비 모델을 검사합니다. 이 파일도 실제 플레이 월드에서는 사용하지 마세요. 두 서버 동기화 모델을 확인하고 검사 전용 확대 비교 화면과 1인칭 화면을 촬영합니다. 배포 JAR에는 비교 화면이나 테스트 클래스를 포함하지 않습니다.

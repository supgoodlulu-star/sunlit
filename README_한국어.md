# 모험가의 수첩 Alpha.4 통합 개발 소스

제공된 Alpha.3.1 성공 소스를 출발점으로 작성한 Forge 프로젝트입니다. 최신 Alpha.4 소스는 제공 자료에 없었고, 사용자의 지시에 따라 확보된 자료만으로 통합했습니다. 검증되지 않은 모드팩 전체 동작을 완료로 표시하지 않기 위해 `Complete` 대신 `Integrated_Source`로 배포합니다.

## 빌드와 설치

- Minecraft 1.20.1, Forge 47.3.0, JDK 17, Gradle Wrapper 8.14.3, ForgeGradle 6.0.54.
- Windows: `gradlew.bat build`
- Linux/macOS: `chmod +x gradlew` 후 `./gradlew build`
- 결과: `build/libs/sunlit-journal-core-0.4.1-dev.jar`
- 기존 Alpha.3.1 JAR은 별도로 보관하고 mods 폴더에서는 새 JAR로 교체합니다. 두 버전은 같은 modId이므로 동시에 설치하지 않습니다.
- 호스트와 접속자 모두 새 모드를 설치합니다. Essential 설정이나 원본 외부 모드 설정을 변경하지 않습니다.
- 기존 월드를 복사해 테스트합니다. 기존 플레이어 칭호 키 `SunlitJournalTitles` 및 `owned`, `equipped` 형식을 유지했습니다.
- 수첩, GUI 텍스처와 아이콘은 JAR에 포함됩니다. `resourcepack/`은 같은 리소스의 선택적 사본이며 별도 필수 설치가 아닙니다.
- 원래 책의 `use_blocky_font: true` 설정을 유지하며 전역 한글 폰트를 덮어쓰지 않습니다. 제공되지 않은 모드팩 전용 폰트 파일을 재생성했다고 주장하지 않습니다.

전용 화면 열기: `/sunlit journal` 또는 `/sunlit journal profile|skills|quests|achievements`.

Patchouli 책 지급:

```mcfunction
/give @s patchouli:guide_book{"patchouli:book":"sunlit_journal_core:sunlit_journal"}
```

책의 각 소개 페이지에 해당 전용 화면을 여는 명령 링크가 있습니다. 기존 `/sunlit titles`, `equip`, `unequip`, `grant`, `revoke` 명령은 유지했습니다.

## 구현한 동작

| 기능 | 구현 |
|---|---|
| 프로필 | 실제 사용자명·Server Nick 현재 닉네임, 입력·초기화, 무제한 변경, 보유 칭호 클릭 장착·해제 |
| 서버 권한 | C2S 요청에서 발신 플레이어 기준 처리, 닉네임 검증, 소유 칭호 검증, 보상 조건 재검사, 패킷 길이·요청 간격 제한 |
| 생활 기술 | Skills 공개 API의 카테고리 ID·레벨·현재 XP·현재 레벨에서 다음 단계에 필요한 XP·해금 스킬 ID를 조회, 화면이 열려 있으면 약 2초마다 갱신 |
| 업무 | 개인별 일일 3개·주간 3개, 다섯 활동 중 서로 다른 무작위 유형과 목표, 완료 보상 수령, 중복 수령 방지 |
| 날짜 | 서버 UTC 일자와 월요일 시작 주차 사용. 날짜가 뒤로 바뀌어도 지난 보상 기간을 다시 열지 않음 |
| 공동 축제 | 오버월드 Serene Seasons 계절에 따라 네 축제 전환, 공동 진행·개인 기여, 기여자 자동 보상, 오프라인 기여자 재접속 보상, 네 계절 칭호·사계요정 |
| 경제 | 개인 UUID 기본 계좌만 조회. 현금·공동 계좌 제외. 잔액 조회 실패 시 조회 불가 표시·판정 보류. 이미 획득한 칭호 유지 |
| 칭호 | 지정된 생활 24종·사계요정 유지, 계절 칭호 4종 추가, 영구 해금·장착 하나, 기존 NBT 복사와 저장 호환 |

생활 활동은 성숙한 CropBlock 파괴, 물고기 태그 낚시 보상, 광석 태그 블록 채광, 식용 아이템 제작·화로 결과 수령, 플레이어가 유발한 동물 번식으로 집계합니다. 크리에이티브·관전 활동은 제외합니다. 놓은 광석을 다시 캐는 방식은 월드에 저장하는 위치 표식으로 제외합니다. 모든 모드의 우클릭 수확·전용 조리 기계·축산 장치 이벤트까지 지원한다고 주장하지 않습니다.

생활 업적 단계는 10/100/1,000/10,000회입니다. 일일 보상은 에메랄드 2개와 XP 20, 주간은 8개와 XP 80, 축제는 16개와 XP 160입니다. 보상 수령 플래그를 먼저 변경하며 인벤토리가 가득 차면 남은 아이템을 떨어뜨립니다. 일반적인 Minecraft 월드 저장을 사용하며 강제 프로세스 종료 중 서로 다른 플레이어/월드 파일 사이의 원자적 트랜잭션을 보장하는 구조는 아닙니다.

`config/sunlit_journal_core-common.toml`의 `debug`로 연동 실패 진단을 켤 수 있고, `festivalGoal`로 다음 축제 목표를 설정합니다. 기본 목표는 1,000입니다.

## 외부 모드 연동 근거와 제한

- Server Nick 1.2.0: 제공 JAR의 `NickManager.get/getNick/setNick`, `NickFormat.sanitize/visibleLength`, `NickConfig.MAX_LENGTH`를 확인했습니다. 명찰 소모·변경 횟수 제한 없이 서버에서 직접 닉네임을 저장·동기화하며 기존 설정 GUI를 호출하지 않습니다.
- Pufferfish Skills 0.16.1: 제공 JAR 공개 API를 조회합니다. 공개 `Skill` 인터페이스는 효과 설명을 제공하지 않습니다. 효과를 추측하지 않고 원본 스킬 트리 열기 버튼을 제공합니다. 기존 트리·XP 계산·해금 조건을 수정하지 않습니다.
- Serene Seasons 9.1.0.2: 제공 JAR의 `SeasonHelper.getSeasonState(Level)`와 `ISeasonState` 메서드 서명을 확인했습니다. 해당 모드의 필수 의존성 GlitchCore가 자료에 없어 실제 계절 모드가 로드된 서버에서는 검증하지 못했습니다. 축제 진행·기여·저장·보상 로직은 서버 테스트에서 별도 검증합니다.
- Numismatics: `ithundxr/CreateNumismatics`의 1.20.1/dev 커밋 `56a5907fd34255c8cfd9a37c116800f7a5d92d3a`(mod_version 1.1.0) 소스를 확인했습니다. `Numismatics.BANK.getAccount(UUID)`로 기존 개인 계좌만 조회하며 계좌를 새로 만들지 않습니다. `Coin.SUN.value`는 4096 Spur이고 `getBalance()`의 int 부분에 별도 long `additionalBalance`를 더합니다. 단위는 실행 시 모드 필드에서 읽고 64비트로 비교합니다. 비공개 초과 잔액 필드를 확인할 수 없는 다른 구현에서는 허위 금액을 표시하지 않습니다.
- 실제 Numismatics 1.1.0/Utils 2.2 JAR은 큰 첨부 파일의 32MiB 전송 제한으로 확보하지 못했습니다. 설치된 JAR·Utils 수정과 위 소스의 일치 여부, 큰 잔액·저장·재접속 동작은 인게임 확인 대상입니다. 기존 Society/Trading/Shipping Bin 동작에 개입하지 않습니다.
- 필수 외부 클래스는 리플렉션으로 격리해 일반 소스 빌드 시 외부 모드 JAR이 필요하지 않습니다. 모드가 없으면 해당 정보를 조회 불가로 표시합니다. 게임의 계절·돈·스킬을 임시 값으로 대체하지 않습니다.

## 자동 검증 실행

```bash
./gradlew build runGameTestServer
```

기본 서버 테스트는 외부 모드 없이 저장·업무·기존 칭호·패킷·축제 로직을 검사합니다. Gradle의 일반 `test`는 JUnit 테스트를 실행하는 구성이 아니며 실제 실행 결과는 GameTest 로그의 테스트 수를 확인해야 합니다.

제공된 JAR 3개를 `provided-mods/`에 둔 경우 실제 API 연동 검사도 실행할 수 있습니다. 외부 바이너리는 소스 ZIP에 재포함하지 않았습니다.

- `servernick-1.2.0.jar`
- `puffish_skills-0.16.1-1.20-forge.jar`
- `Patchouli-1.20.1-85-FORGE.jar`

```bash
./gradlew -PintegrationMods=true -PintegrationModDir=provided-mods build runGameTestServer
```

ForgeGradle은 개발 환경용으로 위 JAR들을 remap하고 Mixin refmap도 개발 매핑에 연결합니다. 검사 월드는 `run-gametest/`에 생성됩니다. 실제 게임 월드를 해당 경로에 두지 마세요. 자세한 실제 결과와 남은 수동 검사는 `VALIDATION.md`를 참조하세요.

## 0.4.1 화면 표시 수정

- 생활 기술 카테고리와 해금 기술 이름은 Pufferfish Skills 0.16.1의 클라이언트 설정에 있는 원본 제목과 현재 리소스팩 번역을 사용합니다.
- 원본 이름을 읽지 못한 Society 카테고리는 수첩의 번역 리소스를 사용합니다. 알 수 없는 기술은 내부 ID 대신 이름 미등록 안내를 표시합니다.
- 번역 리소스가 없는 스킬의 실제 이름을 추측하지 않습니다.
- 빌드와 리소스 검사 통과. 실제 게임 화면에서 원본 모드팩 번역 일치 여부는 추가 확인이 필요합니다.

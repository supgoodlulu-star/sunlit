# 펫 0.9.0-pets-dev 검증

이 문서는 최초 기능 빌드의 기록이다. 현재 0.9.1 외형 개선과 재검증은 [ART_0.9.1.md](ART_0.9.1.md)를 참고한다.

2026-10-29. Forge 1.20.1 / 47.3.0, Java 17. `feature/pet-companions-0.9.0`은 수첩 0.8.6 분기에서 만든 별도 개발 브랜치이다. main과 스킨 미리보기 브랜치는 변경하지 않았다.

## 실제 확인한 범위

- Forge 서버 GameTest **40개 통과**: 기존 수첩 33개(실제 Pufferfish/Server Nick/Patchouli 연동 검사 포함)와 새 펫 7개.
- 펫 테스트: 60종/일반·전설 비율/계절 가중치, 등록 재사용 방지, 재아이템화 후 이전 토큰 거절, 다른 플레이어로 거래, 가득 찬 인벤토리의 지급 취소, 장식 10/20개 가격·영구 해금·소유자 분리, 저장/복원, 임의 개체·원격 상점 요청 거절, 한 마리만 소환, 수동 회수, 무적·통행·농지·압력판·투사체 간섭 방지.
- 실제 통합 클라이언트/서버 패킷: 인형 손에 들기 → 우클릭 등록 → 수첩 프로필 → 보관함 → 소환 → 물리적 장식 가게 열기 → 일반 장식 이리듐 10개 구매 → 착용 → 엔티티 우클릭 쓰다듬기/웃는 눈/하트 → 등록 개체를 인형으로 꺼내기 → 물리적 자판기에서 뽑기권 한 장만 사용 → 인형 지급 → 재등록 → 차원 이동 및 복귀 → 수동 회수. GUI 배율 2와 작은 화면 배율 3 캡처.
- 별도의 **세 번의 실제 클라이언트 종료/재실행**: 소환 상태 저장, 다음 접속에서 같은 개체 한 마리 자동 소환, 수동 회수 후 다음 접속에서 자동 소환하지 않음을 확인.
- 60종 기본/장식 착용 6장의 갤러리를 실제 Minecraft 렌더러로 캡처. UI 목록은 모델 확인용 테스트 갤러리이며 일반 게임 내 도감 화면이 아니다.
- `tools/validate_pets.py`: 60개 모델, 60개 서로 다른 전용 장식 메시, 일반/웃는 눈 레이어, 요정 크기 및 전설 크기, 자판기/가게 텍스처 경로 통과.

## 이미지 구분

`screenshots/pet-*`는 테스트 세계에서 실제 기능을 조작한 게임 화면이다. `screenshots/pets-*`는 실제 게임 렌더러로 60종을 나열한 개발용 모델 갤러리다. 이전 대화의 외형 시트는 디자인 시안이다. 이 세 가지를 같은 결과로 표시하지 않는다.

현재 모델은 **1차 복셀 제작본**이다. 두 발로 서는 아기 용, 작은 요정, 네 계절 5가족, 동물별 큰 형태와 눈, 독립 장식 메시까지 들어갔지만 승인 시안의 풍부한 세부 질감·장식 표현과 같은 완성도라고 판정하지 않았다. 실제 이미지로 이 차이를 검토할 수 있도록 포함한다.

## 아직 전체 모드팩에서 검증하지 않은 부분

- 실제 Numismatics 및 전체 생산/농사/요리 모드를 모두 실행한 최종 테스트가 아니다. 동전/일부 업무 재료는 기존 **개발용 등록 아이템**으로 지급·차감·가격 검사를 했다. 이 대체 아이템은 배포 JAR에 포함하지 않는다. 원본 화폐의 등록 경로와 환율은 명세의 원본 코드 링크로 확인했다.
- Serene Seasons가 없는 환경에서는 계절 우대 없이 전설 20종을 균등 추첨한다. 확률 수학 및 기존 계절 연동 코드는 검사했으나 전체 모드팩의 실제 계절 진행과 전설 파티클 체감은 최종 검증하지 않았다.
- 생활 티켓은 기본 20 TPS, 정상 행동 간격으로 계산한 확률이다. 전체 모드팩의 실제 한 시간 플레이 표본으로 평균 한 장을 측정한 것은 아니다. 지원 범위 및 시간 창/설정은 SPEC.md에 기록했다.
- 보관함은 512개체까지 지원한다. 다수 동시 접속 환경의 성능 부하 테스트와 전용 서버에서 다수 실제 계정의 동시 거래 테스트는 별도이다. 개체 거래/중복 및 토큰 검증은 서버 테스트에서 두 플레이어로 확인했다.

## 재현

```sh
./gradlew build
./gradlew runGameTestServer
# 실제 제공된 Pufferfish/Server Nick/Patchouli JAR가 있을 때
./gradlew -PintegrationMods=true -PintegrationModDir=/path/to/mods runGameTestServer
python tools/validate_pets.py
python tools/generate_pet_models.py
# GUI가 가능한 개발 환경 + 폐기 가능한 SunlitInputTest 세계 필요
./gradlew -I tools/visual-preview/pet-visual.gradle runClient
./gradlew -I tools/visual-preview/pet-integration.gradle runClient
./gradlew -I tools/visual-preview/pet-reconnect.gradle -PreconnectPhase=0 runClient
./gradlew -I tools/visual-preview/pet-reconnect.gradle -PreconnectPhase=1 runClient
./gradlew -I tools/visual-preview/pet-reconnect.gradle -PreconnectPhase=2 runClient
```

이미지 생성 도구로 게임 화면을 대체하지 않았다. 실제 캡처는 `validation/pets-0.9.0/screenshots`에 있다.

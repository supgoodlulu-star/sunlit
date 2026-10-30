# 첨부 Blockbench 여우 적용 — 0.9.2-fox-bb-dev

사용자가 첨부한 `fox.bbmodel`을 기반으로 실제 Forge 1.20.1 펫을 교체한 시험 빌드다. 원본 여우를 새로 창작했다고 표시하지 않는다. 원본의 조형·UV 텍스처·부위 구조와 애니메이션 데이터를 가져오고, Sunlit의 소환·상호작용·장식 기능에 연결했다. 다른 59종은 0.9.1 외형을 유지한다.

## 실제 게임 화면

- [정면·옆·뒤·사선](../../validation/pets-0.9.2/screenshots/fox-bb-angles.png)
- [기본 표정·감은 눈·두루마리 가방·이동 자세](../../validation/pets-0.9.2/screenshots/fox-bb-states.png)
- [월드에 소환한 여우](../../validation/pets-0.9.2/screenshots/fox-pet-world-idle.png)
- [실제 우클릭 쓰다듬기](../../validation/pets-0.9.2/screenshots/fox-pet-world-petting.png)
- [보관함](../../validation/pets-0.9.2/screenshots/fox-pet-vault-summoned.png)
- [장식 가게](../../validation/pets-0.9.2/screenshots/fox-pet-shop-preview.png)

위 이미지는 모두 Minecraft에서 실제 런타임 모델을 렌더링한 캡처다. 각도/표정 판넬은 모델 확인용 개발 화면이며 일반 플레이 화면과 구분한다. 월드/보관함/상점 캡처는 폐기 가능한 테스트 세계의 실제 조작 화면이다. Blender 렌더나 디자인 이미지를 게임 캡처로 대체하지 않았다.

## 파일과 가져온 범위

- `tools/art-sources/pet-models/fox-source.bbmodel`: 첨부 원본 그대로. 3,773개 큐브, 부위 25개, 내장 PNG 2048×2048, 원본 애니메이션 8개.
- `tools/art-sources/pet-models/fox-sunlit.bbmodel`: 편집 가능한 파생 프로젝트. 원본 큐브·UV·텍스처·원본 8개 애니메이션은 유지하고 선택 장식과 감은 눈, 상호작용 애니메이션을 추가.
- `src/main/resources/assets/sunlit_journal_core/pets/fox.json`: 부위 계층, UV가 있는 실행용 메시, 키프레임을 담은 변환 결과.
- `textures/pets/fox_bb_0.png`: 원본에 내장된 텍스처를 손실 없이 추출. 비교용 원본 그림을 별도 평면으로 세워 대신 렌더링하지 않는다.
- `tools/blockbench/import_model.py`: 변환기. 첨부 파일의 설명/메모를 명령으로 실행하지 않는다. 숫자 위치·회전·크기 키프레임만 처리하며 Molang 등 코드를 평가하지 않는다.

원본의 이름/크레딧은 소스 프로젝트에 유지했다. 파생 `.bbmodel`의 기본 실루엣과 표정은 사용자 제공 파일에서 온 것이다.

## 게임 연결

- 기존 `fox` 종 ID를 사용하므로 이미 등록된 여우도 새 외형을 사용한다. 보관함 개체 UUID, 인형 발급 토큰, 장식 해금과 가격, 확률, 업무 보상은 변경하지 않는다.
- 소환체·손에 드는 3D 인형·보관함·상점에서 같은 모델을 사용한다. 실제 전체 높이는 약 0.75블록으로 맞췄다.
- 원본의 초록 목수건과 도토리는 기본 외형이다. 기존 합의대로 **두루마리 가방**만 여우의 유료 영구 장식으로 유지하며 일반 장식 가격은 이리듐 10개다.
- 쓰다듬을 때 원본 눈 부위를 숨기고, 추가한 감은 눈을 표시한다. 머리를 기울이고 꼬리가 움직이며 기존 하트 효과가 나온다. 기본 상태로 돌아가면 원본의 열린 눈이 복구된다.
- 기본 대기·깜빡임·꼬리·귀·고개 움직임을 연결했다. 원본 8개 클립 데이터는 보존하지만 손 흔들기/졸기 등을 모두 자동 재생하는 것은 아니다.
- 원본은 앉은 자세의 모델이다. 따라갈 때 가벼운 도약 모션을 추가했다. 네 발로 서서 걷는 새로운 조형/걷기 애니메이션을 완성했다고 표시하지 않는다.
- 새 클립 `animation.sunlit.follow_hop`, `animation.sunlit.pet` 두 개를 추가했다. 감은 눈 그룹 전환은 게임 렌더러가 처리한다.
- Blockbench 파생 프로젝트에서 `sunlit_scroll_bag` 그룹을 끄면 기본 외형을 볼 수 있다. `sunlit_smile_left/right`는 감은 눈 편집용 숨김 그룹이다. 원본과 파생 프로젝트 모두 텍스처를 내장한다. 실제 Blockbench 앱의 수작업 편집으로 제작했다고 표시하지 않는다.

## 최적화와 남은 제한

같은 부위 안에서 완전히 가려지는 면만 제거한다. 서로 움직이는 부위 사이의 면은 남겨 애니메이션 때 틈이 생기지 않도록 했다. 부분적으로 가려진 면의 UV는 잘라 다시 배열하지 않고 원본대로 보존한다. 원본에 그려진 명암과 작은 큐브마다의 방향광이 겹쳐 거친 줄무늬가 생기는 현상을 줄였다. 월드의 블록/하늘 밝기는 계속 반영한다.

- 추가 부위까지 포함한 입력: 큐브 3,793개, 면 22,758개.
- 완전히 가려져 제거한 면: 6,009개.
- 실행 리소스 전체: 사각형 16,749개, 삼각형 환산 33,498개. 열린 눈·감은 눈·장식이 모두 포함된 합계이며 전부 동시에 표시하지는 않는다.
- 기본 표시 상태(장식 없음): 사각형 16,635개, 삼각형 환산 33,270개.

**앞서 권장한 6,000 triangles 예산을 달성한 저폴리 모델은 아니다.** 이번 버전은 원본 외형 보존을 우선한 여우 한 마리 적용 시험본이다. 다수 소환에 대한 실제 사용자 모드팩 성능과 추가 LOD/UV 정리 검증은 남아 있다. 원본의 정면 중심 텍스처 표현 때문에 옆/뒤의 세부 표현은 정면과 차이가 있으며, 그 부분을 전부 다시 그렸다고 표시하지 않는다.

## 검증

- Forge 1.20.1 / 47.3.0, Java 17: 서버 GameTest **43개 통과**. 기존 40개와 새 숫자 키프레임 보간/루프/종료 처리 3개 포함.
- Python 변환/복셀 메시 검사 **11개 통과**: 내부 면 제거, UV 유지, 서로 다른 부위 보존, 회전 큐브 보존, 코드 형태 키프레임 거절, 원본 보존 등.
- 60종 자산 검사 통과: 여우는 부위/UV 모델, 다른 59종은 기존 복셀 메시로 검증.
- 실제 통합 서버/클라이언트: 인형 우클릭 등록 → 프로필 보관함 → 한 마리 소환 → 물리적 장식 가게에서 이리듐 10개 차감 → 장식 표시 → 우클릭 감은 눈 → 아이템화 → 한 장 뽑기 → 재등록 → 주인 이동 시 따라오기 → 차원 이동 → 수동 회수 검증 통과.
- 배율 2 및 작은 화면 배율 3 캡처. 동전은 테스트 전용 등록 아이템으로 확인했고, 대체 아이템은 배포 JAR에 넣지 않는다. 전체 모드팩 최종 검증을 대신하지 않는다.

## 재생성

```sh
python -m pip install -r tools/requirements-pets.txt
python tools/blockbench/import_model.py
python tools/validate_pets.py
PYTHONPATH=tools python -m unittest blockbench.test_import_model pet_art.test_voxel
./gradlew build
./gradlew runGameTestServer
./gradlew -I tools/visual-preview/fox-model-preview.gradle runClient
# 폐기 가능한 SunlitInputTest 세계와 GUI 환경 필요
./gradlew -I tools/visual-preview/fox-integration.gradle runClient
```

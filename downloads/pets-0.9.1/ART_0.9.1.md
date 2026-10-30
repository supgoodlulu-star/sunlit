# 펫 외형 0.9.1 — 시안 기반 2차 제작

승인된 생활 동물·판타지 생물·사계절 전설 시트를 기준으로 60종의 실제 입체 조형을 다시 만들었다. 현재 작업은 코드로 원본 복셀 조형을 제작하고 Minecraft에서 확인하는 방식이다. Blockbench 전문 작업자가 수작업한 모델이나 외부 3D 생성 서비스의 결과라고 표시하지 않는다.

## 시안과 실제 결과

|분류|승인한 디자인 시트|실제 게임 모델 갤러리|
|---|---|---|
|생활 동물 20종|[시안](../../tools/art-sources/pet-references/life.png)|[게임 렌더링](../../validation/pets-0.9.1/screenshots/pets-life-base.png)|
|판타지 생물 20종|[시안](../../tools/art-sources/pet-references/fantasy.png)|[게임 렌더링](../../validation/pets-0.9.1/screenshots/pets-fantasy-base.png)|
|사계절 전설 20종|[시안](../../tools/art-sources/pet-references/seasons.png)|[게임 렌더링](../../validation/pets-0.9.1/screenshots/pets-legendary-base.png)|

확대 화면: [동물](../../validation/pets-0.9.1/screenshots/detail-animals.png) · [전설](../../validation/pets-0.9.1/screenshots/detail-legends.png) · [판타지](../../validation/pets-0.9.1/screenshots/detail-fantasy.png) · [전설 뒷모습](../../validation/pets-0.9.1/screenshots/detail-rear.png).

위 게임 갤러리와 확대 화면은 Minecraft가 실제 배포용 메시를 렌더링한 개발용 확인 화면이다. 일반 보관함 UI의 화면과는 구분한다. 승인 시트는 게임 화면이 아니다. 갤러리에서는 세부 확인을 위해 종별로 카드 크기에 맞춰 확대한다. 실제 세계에서는 작은 요정과 카피바라 수준 전설의 크기 차이를 유지한다.

## 이번에 바꾼 조형

- 여러 동물이 공유하던 단순 몸통과 머리를 종별 비율로 재구성. 여우의 흰 가슴·어두운 발·풍성한 흰 꼬리, 강아지의 처진 귀·흰 이마·혀, 고양이의 회색 줄무늬·초록 눈·수염·올린 꼬리를 반영.
- 카피바라의 넓은 주둥이와 작은 반쯤 뜬 눈, 바위너구리의 낮은 몸, 친칠라의 큰 둥근 귀, 고슴도치의 가시, 코알라의 큰 코, 물범의 밝은 몸과 수염 등을 각각 제작.
- 판타지 생물의 실루엣을 재구성. 흰 점박이 버섯 갓, 도토리의 거친 모자, 이끼 낀 바위 덩어리, 꽃봉오리·잎 날개, 수정 군집, 열린 조개와 진주, 물방울의 뾰족한 곡선, 회오리의 열린 나선, 깃털 날개, 펼친 책, 태엽 고리 등을 입체로 표현.
- 요정은 금발 곱슬 머리·꽃잎 치마·시즌별 머리 장식과 잎맥이 있는 날개. 용은 두 뒷다리·작은 앞팔·짧은 주둥이·상아색 앞머리 비늘·가지/산호/얼음 뿔·계절 날개와 꼬리 장식을 보강.
- 해태는 밝은 몸과 소용돌이 갈기·계절 꽃·외뿔·돌 부조를, 하늘고래는 밝은 배·넓은 입·작은 눈·양 갈래 지느러미 꼬리를, 백호는 흰 털·줄무늬·파란 눈·계절 목 갈기와 꼬리 꽃을 제작.
- 기본 복셀 간격을 기존의 1/3로 세분화. 같은 평면과 색의 면을 합쳐 불필요한 도형을 줄이는 메시 생성기를 추가.
- 눈과 웃는 눈은 실제 머리 표면을 따라 칠한 색면으로 변경. 눈이 머리에서 돌출되는 방식으로 표현하지 않으며, 쓰다듬을 때만 웃는 표면으로 전환.
- 미리보기의 잘못된 깊이/조명 처리로 생기던 검은 띠를 수정. 장식까지 포함한 회전된 외형의 경계를 계산하여 제목과 버튼을 피하도록 미리보기를 맞춤.

## 검증

- Forge 1.20.1 / 47.3.0에서 서버 GameTest 40개 재통과(실제 Pufferfish/Server Nick/Patchouli 연동 포함).
- 메시 생성기 테스트 4개 통과: 큐브 외피의 면적 보존, 인접 덩어리 내부 면 제거, 눈이 머리를 돌출시키지 않는 표면 채색, 출력 결정성.
- 60종 메시/눈·웃는 눈/60개 서로 다른 장식/실제 크기/블록 텍스처 검사 통과.
- 실제 Minecraft에서 60종 기본·장식 갤러리 6장과 확대/뒷모습 4장 렌더링 확인.
- 새 렌더러와 모델에서 실제 통합 서버/클라이언트의 등록·보관함·소환·구매·장식·쓰다듬기·아이템화·한 장 뽑기·차원 이동·수동 회수 흐름 재검사.

## 현재 판정과 한계

시안의 특징을 반영한 재제작본이며 **시안과 100% 일치한다고 판정하거나 유사도 수치를 제시하지 않는다**. 시트에 없는 뒤쪽·반대편·움직이는 관절은 해석해서 제작했다. 시안의 미세한 털 조각, 빛 표현, 투명 재질 및 모든 표정까지 동일하게 재현한 결과는 아니다. 위 실제 게임 화면으로 차이를 검토할 수 있다.

기존 개체 ID·보관·장식 해금·추첨 확률·업무 보상은 변경하지 않는다. 전체 모드팩 및 다수 플레이어의 고밀도 모델 동시 렌더링에 대한 최종 성능 검증은 별도이다. 0.9.0의 검증 한계도 [기존 검증 문서](VALIDATION.md)에 기록되어 있다.

## 소스와 재생성

`tools/pet_art/sculpt.py`: 종별 조형과 색/무늬.
`tools/pet_art/voxel.py`: 고해상도 복셀 조형·눈 표면 채색·면 병합.
`tools/pet_art/accessories.py`: 종별 전용 장식과 부착 위치.
`src/main/resources/assets/sunlit_journal_core/pets/*.json`: 실제 게임이 사용하는 메시 60개.

이 파일 형식은 현재 커스텀 Forge 렌더러용 메시이며 Blockbench의 `.bbmodel` 프로젝트라고 표시하지 않는다.

```sh
python -m pip install -r tools/requirements-pets.txt
python tools/generate_pet_models.py
python tools/validate_pets.py
PYTHONPATH=tools python -m unittest pet_art.test_voxel
./gradlew build
./gradlew -I tools/visual-preview/pet-visual.gradle runClient
```

# 0.8.2-journal · 원본 스킬 트리 아이콘

생활 기술의 ‘원본 스킬 트리’ 그림을 펼친 손 위에 금빛 마법진이 떠 있는 삽화로 바꿨습니다. 청록색 고리·보랏빛 별·작은 금빛 불꽃을 더하고 기존 생활 분야 그림과 같은 따뜻한 도트 판타지 색감을 사용했습니다.

게임용 투명 PNG는 `textures/gui/illustrations/skill_tree.png`(256×256), 카드에서의 표시 크기는 일반 GUI 64, 좁은 GUI 48픽셀입니다. 다른 생활 분야 그림, 기존 스킬 트리 열기 동작, 실제 레벨 데이터, 업무/계절/보상/저장 로직은 변경하지 않았습니다.

클린 빌드와 실제 Minecraft 렌더러에서 일반/작은 GUI 촬영으로 확인했습니다. 촬영본은 `validation/journal-0.8.2/screenshots`에 있으며 예시 데이터라는 표시를 포함합니다. 전체 모드팩 플레이 검증과 구분합니다. 0.8.1의 서버 테스트 28개 통과 기록은 이전 검증이며 이번 아이콘 변경에서 재실행했다고 주장하지 않습니다.

원본 제작 이미지는 `tools/art-sources/skill-tree.png`, 게임용 PNG는 최근접 축소본입니다. 기본 소스 세트와 배포 JAR에는 미리보기 도구가 들어가지 않습니다.

```sh
./gradlew -I tools/visual-preview/skill-tree-preview.gradle runClient
```

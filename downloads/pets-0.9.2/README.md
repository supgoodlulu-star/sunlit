# Sunlit 0.9.2-fox-bb-dev

첨부한 fox.bbmodel을 기반으로 여우 한 마리를 실제 Forge 펫에 연결한 시험 빌드입니다. 원본 여우를 새로 창작했다고 표시하지 않습니다. 다른 59종은 0.9.1 외형입니다.

- [실제 게임 캡처·가져온 범위·검증·성능 비용](FOX_BLOCKBENCH_0.9.2.md)
- [월드의 여우](../../validation/pets-0.9.2/screenshots/fox-pet-world-idle.png) · [쓰다듬기](../../validation/pets-0.9.2/screenshots/fox-pet-world-petting.png)
- [설치](INSTALL.md) · [기능 명세](SPEC.md)

`sunlit-fox.bbmodel`은 원본에 선택 두루마리 가방·감은 눈·상호작용 모션을 추가한 편집용 프로젝트입니다. 텍스처를 내장합니다. mods 폴더에는 JAR를 사용하며 서버와 클라이언트의 기존 Sunlit JAR를 교체합니다. 기존 등록 여우도 같은 종 ID로 새 외형을 사용합니다.

소환·따라오기·장식 착용·우클릭 쓰다듬기·아이템화·한 장 뽑기·차원 이동의 실제 클라이언트 검사, 서버 GameTest 43개, Python 검사 11개를 통과했습니다. 개발용 동전/재료 대체 아이템은 JAR에 포함하지 않습니다.

이 원본은 고밀도 조형입니다. 완전히 가려진 면 6,009개를 제거했지만 기본 표시 상태 약 33,270 triangles로 이전 6,000 목표보다 높습니다. 다수 동시 소환과 전체 모드팩 성능 검증은 남아 있습니다.

소스 커밋: `a76db9d8016f11e54d023c20bbb9632c637bc43f`

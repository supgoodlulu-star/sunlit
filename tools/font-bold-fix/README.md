# Sunlit Font Bold Fix 1.0.0

Minecraft 1.20.1 / Forge 47.3.0 / Java 17 클라이언트 전용. 기존 Sunlit 모드에 의존하지 않는 별도 모드입니다.

## 설치

1. `sunlit-font-bold-fix-1.0.0.jar`를 클라이언트 `mods`에 추가합니다.
2. `Sunlit_Radio_Ballpen_BoldFix.zip`을 클라이언트 `resourcepacks`에 넣습니다.
3. 게임에서 기존 Radio Ballpen 팩은 끄고 수정 팩을 활성화합니다. 다른 글꼴 팩보다 높은 우선순위에 둡니다.
4. 모드를 처음 설치하면 게임을 재시작하세요. 이후 팩을 바꿀 때는 리소스 새로고침(F3+T)도 지원합니다.

서버에 보정 모드를 설치할 필요가 없습니다. 각 클라이언트에 설치합니다. 기존 Sunlit 게임플레이 모드는 그대로 사용할 수 있습니다.

## 적용 범위

- `sunlit_font:font/page_00.png`부터 `page_99.png` 패턴의 비트맵 공급자에만 적용합니다. 이 팩은 00~09를 사용합니다.
- 원본 글꼴 PNG, 일반 글씨 자간/크기/ascent, 기본 그림자 간격은 바꾸지 않습니다.
- 볼드 덧그림의 수평 이동만 기본 1.0 GUI 픽셀에서 0.25로 줄입니다. 볼드 문자 폭 계산도 같은 간격을 사용합니다.
- 기본 Minecraft 글꼴, 지원하지 않는 글자의 기본 대체 글꼴, 다른 이름 공간의 글꼴에는 적용하지 않습니다.
- 마지막 페이지의 잘못된 `\u0000` 문자열 패딩을 실제 빈 칸 코드 U+0000으로 정리했습니다. 그림 파일은 원본 바이트 그대로이며 실제 지원 문자 집합도 유지합니다.

`config/sunlit_font_bold_fix-client.toml`에서 `boldOffset=0.25`를 조절할 수 있습니다(0~1). 0은 볼드 수평 이동을 없앱니다. 설정 변경 후 재시작하면 확실히 적용됩니다.

## 빌드 및 검증

Java 17로 `./gradlew build`. 완성 JAR는 `build/libs/`에 생성됩니다.

MCP/SRG의 `BitmapProvider.Definition.load`/`m_286048_` 이름은 Forge 47.3.0 매핑에서 확인했습니다. 주입은 클라이언트 목록에만 등록합니다. 리플렉션으로 게임 필드 이름을 추측하지 않으며 별도 refmap에 의존하지 않습니다. QA는 배포 JAR에 포함하지 않습니다.

실제 클라이언트에서 보정값 0.25, 일반 문자 폭/그림자 유지, 기본 및 대체 글꼴 미변경, 리소스 새로고침 후 재적용을 검사했습니다. 별도 개발 클라이언트에서도 완성 JAR의 MixinConfigs 자동 로딩과 ZIP 리소스팩 설치를 확인했습니다. 전체 사용자 모드팩 조합과 Jade 자체는 별도 검증 대상입니다.

QA 실행: 수정 팩의 assets를 별도 폴더로 풀고 `./gradlew -PfontPackDir=/path/to/extracted-pack -I qa/font-check.gradle runClient`. QA 결과는 `validation/fixed/screenshots`에 실제 게임 화면으로 저장됩니다.

MIT 표기는 보정 모드 코드에만 적용됩니다. 글꼴 리소스의 원본 README와 출처 표기는 수정 팩에 그대로 보존했습니다.

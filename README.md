# LukeVanilla 메시징 시스템

Minecraft 네트워크에서 Velocity 프록시와 Paper 서버 간 양방향 메시징 시스템입니다.

## 프로젝트 개요

- **서버 ⇄ 프록시 ⇄ 서버** 메시징: 서버 간 데이터 교환
- **프록시 ⇄ 서버** 메시징: 프록시에서 특정 서버로 명령 전송
- **간편한 API**: 플러그인 개발자를 위한 간단한 메시징 API 제공

## 구성 요소

- **LukeVanilla-Velocity**: Velocity 프록시 플러그인
- **LukeVanilla-Paper**: Paper 서버 플러그인 (템플릿 제공)

## 문서

자세한 내용은 위키 문서를 참조하세요:

- [🔧 설치 및 설정 가이드](wiki/installation.md)
- [📖 사용자 가이드](wiki/user-guide.md)
- [👨‍💻 개발자 API 문서](wiki/developer-api.md)
- [🧪 테스트 및 디버깅](wiki/testing.md)
- [🔄 프로젝트 구조](wiki/project-structure.md)

## 빠른 시작

### 빌드 방법

```bash
# 프로젝트 루트 디렉토리에서
./gradlew shadowJar
```

빌드된 JAR 파일은 `build/libs/` 디렉토리에 생성됩니다.

### 기본 명령어

- `/sendmessage <서버명> <메시지>`: 특정 서버로 메시지 전송
- `/smsg <서버명> <메시지>`: 위 명령어의 단축 버전

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 
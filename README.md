# Music Box (Client) — Minecraft 26.3 / Fabric

마인크래프트 기본 BGM을 골라 듣고, 내 mp3/ogg/wav 파일도 재생하는 **클라이언트 전용** 모드.
서버로 아무것도 보내지 않으므로 **내 화면에서만** 들립니다. 명령어 필요 없음.

## 사용법
- 게임 중 **M 키**, 또는 타이틀/일시정지 화면 왼쪽 위 **♪ 음악** 버튼
- **마크 기본 곡** 탭: 클릭하면 재생
- **내 파일** 탭: `.minecraft/musicbox/` 폴더에 넣은 파일을 자동 인식 (`폴더 열기` / `새로고침` 버튼)
- 정지 · 일시정지 · 반복 · 볼륨, 반복이 꺼져 있으면 다음 곡으로 자동 이어짐

## 설치
1. Fabric Loader 0.19.5+ 와 Fabric API (26.3) 설치, Java 25
2. [Releases](../../releases)의 jar를 `mods` 폴더에 넣기

## 빌드
```
gradle build        # JDK 25 필요, 결과: build/libs/
```
GitHub Actions가 push마다 자동 빌드하고, `v1.0.0` 같은 태그를 푸시하면 Release에 jar를 올립니다.

## GitHub에 올리기
```
git init && git add . && git commit -m "init"
git branch -M main
git remote add origin https://github.com/<아이디>/<저장소>.git
git push -u origin main
git tag v1.0.0 && git push origin v1.0.0   # 릴리스 자동 생성
```

## 라이선스
MIT. 내 음악 파일의 저작권은 사용자 본인 책임입니다.

# 산출물 재생성 방법

산출물 3종은 상위 폴더(`deliverables/`)에 있고, 이 폴더는 PDF를 다시 만드는 스크립트입니다.

```bash
# 1) 앱 개발 서버를 5174 포트로 띄운 상태에서 화면 캡처 + 강조 영역 좌표 기록
BASE=http://localhost:5174 node shoot.mjs      # → shots/, annots.json

# 2) 슬라이드 HTML 생성
python3 build_deck.py                          # → deck.html  (제출용 25쪽)
python3 build_talk.py                          # → talk.html  (발표용 14쪽)

# 3) PDF 인쇄 (headless Chrome)
IN=deck.html OUT=deck.pdf node print.mjs
IN=talk.html OUT=talk.pdf node print.mjs
```

- `spec.mjs` : UI 흐름도에서 빨간 점선으로 감쌀 버튼/영역 정의. 좌표는 캡처 시 실제 DOM에서 측정한다.
- 설명 번호는 각 슬라이드 note 순서(1번부터)이며, `build_*.py`의 화면별 `{key: 번호}` 매핑으로 연결한다.

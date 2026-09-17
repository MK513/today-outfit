# 발표용(5분) 슬라이드 — 8반_김민_오늘뭐입지-발표.pdf
# ERD 도식 · 캡처 주석 렌더링은 build_deck.py를 재사용한다.
import build_deck as bd
from pathlib import Path

HERE = Path(__file__).parent
OUT = HERE / "talk.html"
esc, rich, overlay = bd.esc, bd.rich, bd.overlay

CSS = bd.CSS + """
.slide { padding: 48px 64px 40px; }
.title { font-size: 36px; }
.num { font-size: 17px; padding: 6px 11px; }
.sub { font-size: 18px; margin: 4px 0 26px 2px; }
.foot { left: 64px; right: 64px; }
.shot-wrap img { height: 500px; border: 2px solid #C7D2E8; }
.shot .cap { font-size: 15px; }
.note { font-size: 17px; line-height: 1.5; padding: 14px 16px; border-radius: 10px; }
.note code { font-size: 15px; }
.tag { font-size: 13px; padding: 3px 9px; }
.nnum { width: 24px; height: 24px; font-size: 15px; }
.notes { gap: 12px; justify-content: center; }
.flow { align-items: center; }
"""

pages = []


def slide(num, title, sub, body, legend=False, center=False):
    n = len(pages) + 1
    if center:
        body = f'<div style="height:520px; display:flex; flex-direction:column; justify-content:center; margin-top:-30px;">{body}</div>'
    head = f'<div class="head"><span class="num">{num}</span><span class="title">{esc(title)}</span></div>'
    subh = f'<p class="sub">{esc(sub)}</p>' if sub else ""
    leg = (
        '<div class="legend" style="top:52px; right:64px;"><span class="tag t-ui">UI 이동</span><span class="tag t-api">API</span>'
        '<span class="tag t-err">예외</span></div>'
        if legend else ""
    )
    foot = f'<div class="foot"><span>오늘 뭐 입지</span><span>8반 김민 · {n}</span></div>'
    pages.append(f'<section class="slide">{leg}{head}{subh}{body}{foot}</section>')


# 1. 표지
pages.append(bd.pages[0].replace("프로젝트 기술서", "발표 자료"))

# 2. 문제 → 해결
pains = [
    ("내 옷을 한눈에 모른다", "사진 · 문장으로 AI가 채우는 디지털 옷장"),
    ("매일 아침 코디 고민", "오늘의 픽 + 상황별 AI 코디 추천"),
    ("한 주 코디 계획이 없다", "주간 플래너 + AI 주간 추천"),
]
rows = "".join(
    f'<div style="display:flex; align-items:center; gap:28px;">'
    f'<div class="card" style="flex:1; background:#fff; padding:26px 30px;"><p style="margin:0; font-size:14px; font-weight:800; color:#e5383b;">PROBLEM</p>'
    f'<p style="margin:6px 0 0; font-size:28px; font-weight:800;">{p}</p></div>'
    f'<div style="font-size:40px; font-weight:800;">→</div>'
    f'<div class="card" style="flex:1.2; background:#1D2B50; border-color:#1D2B50; color:#fff; padding:26px 30px;"><p style="margin:0; font-size:14px; font-weight:800; color:#A9C0EA;">SOLUTION</p>'
    f'<p style="margin:6px 0 0; font-size:26px; font-weight:700;">{s}</p></div></div>'
    for p, s in pains
)
slide("01", "왜 만들었나", "\"옷은 많은데 입을 옷이 없다\"",
      f'<div style="display:flex; flex-direction:column; gap:22px; margin-top:10px;">{rows}</div>', center=True)

# 3. 서비스 한눈에
feats = [
    ("CLOSET", "옷장", "AI로 빠른 의류 등록"),
    ("HOME", "오늘의 픽", "잠금 뽑기로 빠른 결정"),
    ("OUTFIT", "코디", "상황 입력 → AI 추천"),
    ("PLANNER", "주간 플래너", "날짜별 코디 배치"),
    ("CHALLENGE", "챌린지", "좌/우 선택 + AI 평가"),
]
tiles = "".join(
    f'<div class="card" style="flex:1; background:#fff; padding:26px 20px; text-align:center;">'
    f'<p style="margin:0; font-size:13px; font-weight:800; letter-spacing:.1em; color:#999;">{a}</p>'
    f'<p style="margin:10px 0 12px; font-size:27px; font-weight:800;">{b}</p><p style="margin:0; font-size:17px; color:#555;">{c}</p></div>'
    for a, b, c in feats
)
slide("01", "오늘 뭐 입지는", "",
      f'''<div style="background:#1D2B50; color:#fff; border-radius:14px; padding:38px 44px; margin:10px 0 34px;">
      <p style="margin:0; font-size:34px; font-weight:800; line-height:1.45;">내가 <span style="color:#A9C0EA;">이미 가진 옷</span> 안에서<br/>AI가 오늘과 이번 주 코디를 골라주는 서비스</p></div>
      <p style="margin:-18px 0 26px; font-size:19px; color:#555;">타겟 사용자 — 옷은 있지만 매일 코디 결정에 시간을 쓰는 <b>20~30대 직장인 · 학생</b></p>
      <div style="display:flex; gap:14px;">{tiles}</div>''', center=True)

# 4. 액터
actors = [
    ("비회원", "회원가입 · 로그인 · 데모 체험"),
    ("회원", "옷장 · 코디 · 플래너 · 챌린지 · 탈퇴"),
    ("AI 모델", "의류 분석 · 코디 추천 · 평가 (외부 시스템)"),
    ("파일 저장소", "의류 사진 보관 (외부 시스템)"),
]
acards = "".join(
    f'<div class="card" style="background:{"#1D2B50" if i < 2 else "#fff"}; color:{"#fff" if i < 2 else "#1D2B50"}; border-color:{"#1D2B50" if i < 2 else "#ddd"}; padding:34px 34px;">'
    f'<p style="margin:0; font-size:14px; font-weight:800; color:{"#aaa" if i < 2 else "#999"};">{"사용자" if i < 2 else "외부 시스템"}</p>'
    f'<p style="margin:8px 0 12px; font-size:32px; font-weight:800;">{a}</p><p style="margin:0; font-size:20px; line-height:1.5;">{b}</p></div>'
    for i, (a, b) in enumerate(actors)
)
slide("02", "시스템 액터", "사용자 2종 + 서버가 연동하는 외부 시스템 2종",
      f'<div style="display:grid; grid-template-columns:1fr 1fr; gap:18px;">{acards}</div>', center=True)


# 5. 요구사항 — 액터별 기능
reqs = [
    ("비회원", "인증", "회원가입 · 로그인 · 데모 계정 체험", "S01 S02", "FR-01~02"),
    ("회원", "홈 · 오늘의 픽", "슬롯별 랜덤 뽑기 · 잠금 후 재뽑기 · 오늘 코디 확정", "S03", "FR-04~05"),
    ("회원", "옷장", "사진 AI 등록 · 문장 AI 일괄 등록 · 직접 입력 · 필터 조회 · 상세 · 삭제", "S04~S08", "FR-06~10"),
    ("회원", "코디", "상황 기반 AI 추천 · 직접 만들기(순서 지정) · 상세 · 삭제", "S09~S12", "FR-11~14"),
    ("회원", "주간 플래너", "날짜별 배치 · 교체 · 해제 · AI 주간(1~7일) 추천 일괄 저장", "S13", "FR-15~16"),
    ("회원", "챌린지", "좌/우 선택 라운드 · AI 점수 · 코멘트 · 태그 평가 후 저장", "S14 S15", "FR-17~18"),
    ("회원", "계정", "로그아웃 · 계정 탈퇴(개인 데이터 전체 삭제)", "S16", "FR-03"),
    ("AI 모델 (외부)", "분석 · 추천 · 평가", "사진 인식 · 문장 파싱 · 코디 추천 · 주간 추천 · 코디 평가", "—", "AI 5"),
    ("파일 저장소 (외부)", "사진 보관", "의류 사진 업로드 · 삭제, image_url 제공", "—", "API 1"),
]
cell = "padding:9px 12px; font-size:15.5px;"
rrows = "".join(
    f'<tr><td style="{cell} font-weight:800; width:150px; white-space:nowrap;">{a}</td>'
    f'<td style="{cell} width:132px; font-weight:600;">{b}</td><td style="{cell}">{esc(c)}</td>'
    f'<td class="mono" style="{cell} width:104px; font-weight:700; white-space:nowrap;">{d}</td>'
    f'<td class="mono" style="{cell} width:92px; color:#b3261e; font-weight:700; white-space:nowrap;">{e}</td></tr>'
    for a, b, c, d, e in reqs
)
slide("03", "요구사항 — 액터별 기능", "기능 18개(FR-01~18) · 모든 기능은 화면(S01~S16)과 API에 1:1로 연결",
      f'<table style="margin-top:-6px;"><tr><th style="padding:10px 12px;">액터</th><th style="padding:10px 12px;">영역</th>'
      f'<th style="padding:10px 12px;">주요 기능</th><th style="padding:10px 12px;">화면</th><th style="padding:10px 12px;">요구사항 ID</th></tr>{rrows}</table>')

# 6. 전체 화면 흐름
def chip(name, dark=False):
    return (f'<div style="border:2px solid #1D2B50; border-radius:10px; padding:12px 8px; text-align:center; font-size:18px; font-weight:800; '
            f'background:{"#1D2B50" if dark else "#fff"}; color:{"#fff" if dark else "#1D2B50"};">{name}</div>')


down = '<div style="text-align:center; color:#2B4C8C; font-size:20px; font-weight:800; line-height:1.2;">↓</div>'
cols = [
    ("HOME", ["홈 · 오늘의 픽", "코디 상세"]),
    ("CLOSET", ["내 옷장", "AI · 직접 등록", "의류 상세"]),
    ("OUTFIT", ["내 코디", "AI 추천 · 직접", "코디 상세"]),
    ("PLANNER", ["주간 플래너", "배치 · AI 추천"]),
    ("CHALLENGE", ["챌린지", "결과 · AI 평가"]),
]
colhtml = "".join(
    f'<div style="flex:1; background:#F4F7FC; border-radius:12px; padding:14px;"><p style="margin:0 0 10px; text-align:center; font-size:13px; font-weight:800; letter-spacing:.1em; color:#888;">{t}</p>'
    + down.join(chip(x, i == 0) for i, x in enumerate(items)) + "</div>"
    for t, items in cols
)
slide("04", "전체 화면 흐름", "16개 화면 · 로그인 후 하단 탭 5개로 이동",
      f'''<div style="display:flex; gap:22px; align-items:center;">
      <div style="width:190px;">{chip("로그인", True)}<div style="text-align:center; font-size:15px; color:#888; margin:8px 0;">회원가입 · 데모 체험</div>
      <div style="text-align:center; color:#2B4C8C; font-size:30px; font-weight:800;">→</div></div>
      <div style="flex:1; display:flex; gap:12px; align-items:flex-start;">{colhtml}</div></div>
      <p style="margin:30px 0 0; font-size:18px; color:#555; text-align:center;">상단 ⚙ 설정 → 로그아웃 · 계정 탈퇴</p>''', center=True)


# 6~10. 핵심 화면 흐름
def ui(title, sub, shots, notes):
    imgs = []
    for i, (f, cap, mapping) in enumerate(shots):
        if i:
            imgs.append('<div class="arrow">▶</div>')
        imgs.append(
            f'<div class="shot"><div class="shot-wrap"><img src="{(bd.SHOTS / (f + ".png")).as_uri()}"/>{overlay(f, mapping)}</div>'
            f'<div class="cap">{esc(cap)}</div></div>'
        )
    kinds = {"ui": ("t-ui", "UI 이동"), "api": ("t-api", "API"), "err": ("t-err", "예외")}
    nh = ""
    for i, (k, t) in enumerate(notes, start=1):
        nh += (f'<div class="note {k}"><span class="nnum">{i}</span>'
               f'<span><span class="tag {kinds[k][0]}">{kinds[k][1]}</span><br/>{rich(t)}</span></div>')
    slide("04", title, sub, f'<div class="flow" style="margin-top:-8px;"><div class="shots">{"".join(imgs)}</div><div class="notes">{nh}</div></div>', legend=True)


ui("홈 – 오늘의 픽", "로그인 → 뽑기 → 오늘 코디 확정",
   [("01_login", "로그인", {"login": 1, "demo": 1}),
    ("04_home_drawn", "뽑기 · 잠금", {"lock": 2, "reroll": 2, "confirm": 3}),
    ("05_home_confirmed", "확정", {"chip": 3})],
   [("ui", "로그인 · 데모 체험 → 홈"),
    ("api", "`POST /home/today-pick` 잠금한 옷은 유지하고 다시 뽑기"),
    ("api", "확정 → `POST /outfits` + `PUT /planner/schedules/{오늘}`")])

ui("옷장 – AI로 등록", "사진 한 장, 문장 한 줄로 등록",
   [("08b_add_photo_result", "사진 AI 분석", {"result": 1, "save": 1}),
    ("09_add_photo_fail", "분석 실패", {"manual": 3}),
    ("10_add_text_candidates", "문장 → 여러 벌", {"analyze": 2, "save": 2})],
   [("api", "`POST /ai/clothes/photo-analysis` 후보 확인 · 수정 후 저장"),
    ("api", "`POST /ai/clothes/text-parse` 여러 벌 일괄 저장"),
    ("err", "AI 실패(502) → 수동 등록으로 이동")])

ui("코디 – 상황별 AI 추천", "상황 입력 → 추천 → 저장",
   [("14_outfit_ai_result", "AI 코디 추천", {"situation": 1, "save": 2}),
    ("16_outfit_detail", "코디 상세", {"reason": 1, "place": 2})],
   [("api", "`POST /ai/outfits/recommend` 코디 + 추천 이유"),
    ("ui", "저장 → 코디 상세 → 플래너에 배치"),
    ("err", "보유 의류가 없으면 빈 상태 안내 (422)")])

ui("주간 플래너", "날짜별 배치 + AI 주간 추천",
   [("17_planner", "주간 플래너", {"empty": 1, "filled": 1}),
    ("18_planner_picker", "코디 선택", {"card": 1}),
    ("20_planner_ai", "AI 주간 추천", {"request": 2, "save": 2})],
   [("api", "`PUT /planner/schedules/{date}` 신규 201 / 교체 200"),
    ("api", "`POST /ai/planner/weekly-recommend` 1~7일 추천 → 전체 저장"),
    ("err", "옷이 부족하면 가능한 날짜만 생성")])

ui("코디 챌린지", "좌/우 선택 게임 → AI 평가",
   [("22_challenge_round", "좌/우 선택", {"left": 1, "blur": 1}),
    ("23_challenge_result", "AI 평가 결과", {"score": 2, "save": 2})],
   [("ui", "카테고리별 마음에 드는 옷 선택"),
    ("api", "`POST /ai/outfits/evaluate` 점수 · 코멘트 · 태그"),
    ("err", "평가 실패 시에도 점수 없이 저장 가능")])

# 11. ERD
epoints = [
    ("M:N 해소", "코디 ↔ 의류를 outfit_items로 분리"),
    ("하루 1코디", "(user_id, plan_date) UNIQUE"),
    ("탈퇴 시 전체 삭제", "모든 개인 데이터 CASCADE"),
]
ep = "".join(f'<div class="note" style="flex:1; font-size:16px;"><b style="font-size:18px;">{a}</b><br/>{esc(b)}</div>' for a, b in epoints)
slide("05", "데이터 모델", "7개 엔티티 — 사용자 · 의류 · 코디 · 플래너 중심",
      f'<div style="border:1px solid #eee; border-radius:10px; padding:6px 0 0; background:#fcfcfc; margin-top:-10px;">{bd.erd_svg()}</div>'
      f'<div style="display:flex; gap:14px; margin-top:14px;">{ep}</div>')

# 12. API
groups = [("Auth", 4), ("User", 2), ("Home", 2), ("Clothes", 6), ("Outfits", 4), ("Planner", 4), ("Challenge", 1), ("AI", 5)]
gchips = "".join(
    f'<div style="display:flex; justify-content:space-between; border-bottom:1px solid #E3E9F5; padding:10px 4px; font-size:19px;">'
    f'<b>{g}</b><span class="muted">{c}</span></div>'
    for g, c in groups
)
sample_apis = [
    ("POST", "/auth/login", "로그인 · JWT 발급"),
    ("GET", "/clothes", "옷장 목록 (필터)"),
    ("POST", "/ai/clothes/photo-analysis", "사진 AI 분석"),
    ("POST", "/ai/outfits/recommend", "상황 기반 코디 추천"),
    ("POST", "/outfits", "코디 저장"),
    ("PUT", "/planner/schedules/{date}", "날짜 배치 · 교체"),
]
arows = "".join(
    f'<tr><td style="padding:12px 12px; width:88px;"><span class="pill {m}" style="font-size:13px; padding:4px 9px;">{m}</span></td>'
    f'<td class="mono" style="padding:12px 12px; font-size:16.5px; font-weight:700;">{esc(path)}</td>'
    f'<td style="padding:12px 12px; font-size:16px; width:210px;">{esc(desc)}</td></tr>'
    for m, path, desc in sample_apis
)
slide("06", "API 명세", "OpenAPI 3.0.3 · 화면별로 필요한 API 전체 정의 (상세는 8반_김민_오늘뭐입지-API.yml)",
      f'<div style="display:flex; gap:44px; align-items:flex-start;">'
      f'<div style="width:290px;"><p style="margin:0; font-size:72px; font-weight:800; letter-spacing:-0.04em;">28<span style="font-size:28px;"> 개</span></p>'
      f'<p class="muted" style="margin:0 0 14px; font-size:17px;">REST API</p>{gchips}</div>'
      f'<div style="flex:1;"><p style="margin:6px 0 10px; font-size:17px; font-weight:800;">대표 API 6개</p>'
      f'<table>{arows}</table></div></div>', center=True)

# 13. 고려사항
cons = [
    ("저장 시점", "AI 오인식이 쌓이지 않도록\n후보 → 확인 → 저장"),
    ("AI 지연 · 비용", "타임아웃 · 재시도\n일일 호출 한도 (429)"),
    ("다음 단계", "목업 프론트엔드를\n실제 API로 연동"),
]
cc = "".join(
    f'<div class="card" style="flex:1; background:{"#1D2B50" if i == 2 else "#fff"}; color:{"#fff" if i == 2 else "#1D2B50"}; border-color:{"#1D2B50" if i == 2 else "#ddd"}; padding:40px 32px;">'
    f'<p style="margin:0; font-size:40px; font-weight:800; color:{"#A9C0EA" if i == 2 else "#ccc"};">{i + 1:02d}</p>'
    f'<p style="margin:14px 0 14px; font-size:30px; font-weight:800;">{a}</p><p style="margin:0; font-size:20px; line-height:1.6;">{esc(b).replace(chr(10), "<br/>")}</p></div>'
    for i, (a, b) in enumerate(cons)
)
slide("07", "고려사항", "설계하며 고민한 점과 다음 단계", f'<div style="display:flex; gap:18px; margin-top:20px;">{cc}</div>', center=True)

# 15. 마무리
pages.append(
    """<section class="slide" style="padding:0; background:#1D2B50; color:#fff;">
  <div style="position:absolute; right:-120px; top:-120px; width:620px; height:620px; border-radius:50%; background:#1f1f1f;"></div>
  <div style="position:absolute; left:90px; top:230px;">
    <h1 style="margin:0; font-size:72px; font-weight:800; letter-spacing:-0.04em;">감사합니다</h1>
    <p style="margin:18px 0 0; font-size:24px; color:#bbb;">오늘 뭐 입지 · 8반 김민</p>
  </div>
</section>"""
)

OUT.write_text(
    f'<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>오늘 뭐 입지 발표</title><style>{CSS}</style></head><body>{"".join(pages)}</body></html>',
    encoding="utf-8",
)
print("talk pages:", len(pages))

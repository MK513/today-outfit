# 8반_김민_오늘뭐입지-개요.pdf (제출용) 슬라이드 생성기
import html, json
from pathlib import Path

HERE = Path(__file__).parent
SHOTS = HERE / "shots"
ANN = json.loads((HERE / "annots.json").read_text(encoding="utf-8"))
OUT = HERE / "deck.html"

TEAM = "8반 김민"
SERVICE = "오늘 뭐 입지"
SHOT_H = 520  # 화면 캡처 표시 높이(px), 원본 460x920의 1/2 비율

CSS = """
@page { size: 1280px 720px; margin: 0; }
* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
body { font-family: 'Apple SD Gothic Neo', 'Pretendard', 'Malgun Gothic', sans-serif; color: #1D2B50; -webkit-print-color-adjust: exact; print-color-adjust: exact; letter-spacing: -0.01em; }
.slide { width: 1280px; height: 720px; position: relative; overflow: hidden; page-break-after: always; background: #fff; padding: 40px 56px 36px; }
.slide:last-child { page-break-after: auto; }
.head { display: flex; align-items: center; gap: 14px; margin-bottom: 6px; }
.num { background: #1D2B50; color: #fff; font-weight: 800; font-size: 15px; border-radius: 6px; padding: 5px 9px; }
.title { font-size: 28px; font-weight: 800; letter-spacing: -0.03em; }
.sub { color: #777; font-size: 14px; margin: 0 0 18px 2px; }
.foot { position: absolute; left: 56px; right: 56px; bottom: 16px; display: flex; justify-content: space-between; font-size: 11px; color: #aaa; border-top: 1px solid #eee; padding-top: 8px; }
.mono { font-family: 'SF Mono', Menlo, monospace; font-size: 0.92em; }
table { border-collapse: collapse; width: 100%; }
th { background: #1D2B50; color: #fff; font-weight: 700; font-size: 13px; padding: 8px 10px; text-align: left; }
td { border-bottom: 1px solid #e6e6e6; font-size: 13px; padding: 7px 10px; vertical-align: top; line-height: 1.45; }
.pill { display: inline-block; font-size: 11px; font-weight: 800; padding: 2px 7px; border-radius: 4px; color: #fff; white-space: nowrap; }
.GET { background: #6E86BE; } .POST { background: #2B4C8C; } .PUT { background: #7C8FBF; } .DELETE { background: #E03131; }
.card { border: 1px solid #e3e3e3; border-radius: 10px; padding: 16px 18px; background: #fafafa; }
.muted { color: #777; }
.legend { position: absolute; top: 40px; right: 56px; display: flex; gap: 8px; font-size: 12px; }
.tag { display: inline-block; font-size: 11px; font-weight: 800; padding: 2px 7px; border-radius: 4px; color: #fff; margin-right: 6px; white-space: nowrap; }
.t-ui { background: #2B4C8C; } .t-api { background: #1D2B50; } .t-err { background: #fff; color: #2B4C8C; border: 1.5px solid #9DB0D8; }
.flow { display: flex; gap: 22px; align-items: flex-start; }
.shots { display: flex; gap: 12px; align-items: flex-start; }
.shot { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.shot-wrap { position: relative; }
.shot-wrap img { height: 520px; display: block; border-radius: 10px; border: 2px solid #C7D2E8; }
.shot-wrap svg { position: absolute; inset: 0; width: 100%; height: 100%; overflow: visible; }
.shot .cap { font-size: 12px; font-weight: 700; }
.arrow { align-self: center; font-size: 22px; color: #2B4C8C; margin-top: -30px; }
.notes { flex: 1; display: flex; flex-direction: column; gap: 7px; }
.note { font-size: 13px; line-height: 1.5; padding: 7px 10px 7px 12px; border-radius: 0 8px 8px 0; background: #F4F7FC; border-left: 4px solid #9DB0D8; display: flex; gap: 8px; align-items: flex-start; }
.note.api { border-left-color: #1D2B50; } .note.ui { border-left-color: #2B4C8C; } .note.err { border-left-color: #9DB0D8; background: #F7F8FA; }
.note code { font-family: 'SF Mono', Menlo, monospace; font-size: 12px; font-weight: 700; color: #2B4C8C; }
.nnum { flex: 0 0 auto; width: 19px; height: 19px; border-radius: 50%; background: #E03131; box-shadow: 0 0 0 2px #fde7e7; color: #fff; font-size: 12px; font-weight: 800; display: flex; align-items: center; justify-content: center; margin-top: 1px; }
.nnum.blank { background: #c9c9c9; }
"""


def esc(s):
    return html.escape(s)


def rich(s):
    parts = esc(s).split("`")
    return "".join(f"<code>{p}</code>" if i % 2 else p for i, p in enumerate(parts))


# ---------------------------------------------------------------- 캡처 위 주석(점선 사각형 + 번호 화살표)
def overlay(shot, mapping):
    """mapping: {강조영역 key: 설명 번호}"""
    boxes = ANN.get(shot, {})
    p = []
    for key, num in mapping.items():
        b = boxes.get(key)
        if not b:
            continue
        x, y, w, h, d = b["x"], b["y"], b["w"], b["h"], b["dir"]
        gap = 46
        if d == "right":
            bx, by, ex, ey = x + w + gap, y + h / 2, x + w + 5, y + h / 2
            head = f"{ex},{ey} {ex + 10},{ey - 6} {ex + 10},{ey + 6}"
        elif d == "left":
            bx, by, ex, ey = x - gap, y + h / 2, x - 5, y + h / 2
            head = f"{ex},{ey} {ex - 10},{ey - 6} {ex - 10},{ey + 6}"
        elif d == "up":
            bx, by, ex, ey = x + w / 2, y - gap, x + w / 2, y - 5
            head = f"{ex},{ey} {ex - 6},{ey - 10} {ex + 6},{ey - 10}"
        else:
            bx, by, ex, ey = x + w / 2, y + h + gap, x + w / 2, y + h + 5
            head = f"{ex},{ey} {ex - 6},{ey + 10} {ex + 6},{ey + 10}"
        bx = min(max(bx, 24), 436)
        by = min(max(by, 24), 896)
        p.append(f'<rect x="{x - 3}" y="{y - 3}" width="{w + 6}" height="{h + 6}" rx="7" fill="none" stroke="#fff" stroke-width="8" opacity="0.85"/>')
        p.append(f'<rect x="{x - 3}" y="{y - 3}" width="{w + 6}" height="{h + 6}" rx="7" fill="none" stroke="#E03131" stroke-width="4" stroke-dasharray="9 6"/>')
        p.append(f'<line x1="{bx}" y1="{by}" x2="{ex}" y2="{ey}" stroke="#fff" stroke-width="8" opacity="0.85"/>')
        p.append(f'<line x1="{bx}" y1="{by}" x2="{ex}" y2="{ey}" stroke="#E03131" stroke-width="4"/>')
        p.append(f'<polygon points="{head}" fill="#E03131"/>')
        p.append(f'<circle cx="{bx}" cy="{by}" r="19" fill="#E03131" stroke="#fff" stroke-width="3"/>')
        p.append(f'<text x="{bx}" y="{by + 8}" font-size="24" font-weight="800" fill="#fff" text-anchor="middle" font-family="Apple SD Gothic Neo, sans-serif">{num}</text>')
    if not p:
        return ""
    return f'<svg viewBox="0 0 460 920" xmlns="http://www.w3.org/2000/svg">{"".join(p)}</svg>'


pages = []
page_no = [0]


def slide(num, title, sub, body, legend=False):
    page_no[0] += 1
    head = f'<div class="head"><span class="num">{num}</span><span class="title">{esc(title)}</span></div>' if num else ""
    subh = f'<p class="sub">{esc(sub)}</p>' if sub else ""
    leg = (
        '<div class="legend"><span><span class="tag t-ui">UI 이동</span></span>'
        '<span><span class="tag t-api">API · 기능</span></span><span><span class="tag t-err">예외 · 모달</span></span>'
        '</div>'
        if legend else ""
    )
    foot = f'<div class="foot"><span>{SERVICE} · AI 웹 서비스 설계 Mini-project</span><span>{TEAM} · {page_no[0]}</span></div>'
    pages.append(f'<section class="slide">{leg}{head}{subh}{body}{foot}</section>')


# ---------------------------------------------------------------- 표지
page_no[0] += 1
pages.append(
    """
<section class="slide" style="padding:0; background:#1D2B50; color:#fff;">
  <div style="position:absolute; right:-120px; top:-120px; width:620px; height:620px; border-radius:50%; background:#25355E;"></div>
  <div style="position:absolute; right:120px; bottom:-200px; width:420px; height:420px; border-radius:50%; background:#2C3D68;"></div>
  <div style="position:absolute; left:90px; top:150px;">
    <p style="margin:0; font-size:15px; letter-spacing:0.2em; color:#aaa; font-weight:700;">TODAY OUTFIT</p>
    <h1 style="margin:14px 0 0; font-size:78px; font-weight:800; letter-spacing:-0.04em;">오늘 뭐 입지</h1>
    <p style="margin:18px 0 0; font-size:26px; font-weight:600; color:#ddd;">AI 기반 옷장 관리 · 코디 추천 · 주간 플래너 서비스</p>
    <div style="margin-top:90px; font-size:17px; line-height:1.9; color:#bbb;">
      웹서비스 설계 Mini-project · 프로젝트 기술서<br/>
      <b style="color:#fff;">8반 김민</b> · 2026.09.17
    </div>
  </div>
</section>"""
)

# ---------------------------------------------------------------- 목차
toc = [
    ("01", "서비스 개요", "배경 · Pain Point · 목적 · 핵심 가치 · 주요 기능"),
    ("02", "시스템 액터 정의", "액터 식별 및 액터별 제공 기능"),
    ("03", "요구사항 정의", "액터별 기능 목록 ↔ 화면 ↔ API 매핑"),
    ("04", "UI 흐름도", "전체 화면 구조 및 화면별 동작 · API · 예외 시나리오"),
    ("05", "데이터 모델 설계", "엔티티 · 속성 · 관계 (ERD)"),
    ("06", "API 명세 정의", "OAS 기반 API 목록 · 공통 스키마 · 에러 코드"),
    ("07", "고려사항", "설계 상의 고민 · 다음 단계 개발 시 고려 사항"),
]
rows = "".join(
    f'<div style="display:flex; align-items:baseline; gap:22px; padding:13px 0; border-bottom:1px solid #eee;">'
    f'<span style="font-size:30px; font-weight:800; width:52px;">{n}</span>'
    f'<span style="font-size:22px; font-weight:800; width:260px;">{t}</span><span class="muted" style="font-size:15px;">{d}</span></div>'
    for n, t, d in toc
)
slide(None, "", "", f'<div style="display:flex; gap:60px; margin-top:20px;"><div style="width:260px;"><p style="font-size:46px; font-weight:800; margin:40px 0 0; letter-spacing:-0.03em;">CONTENTS</p></div><div style="flex:1;">{rows}</div></div>')

# ---------------------------------------------------------------- 01 서비스 개요
pains = [
    ("내가 가진 옷을 한눈에 파악하기 어렵다",
     "옷장 속 옷을 기억에 의존 → 비슷한 옷 중복 구매, 잊힌 옷은 입지 않게 됨. 한 벌씩 기록하는 앱은 입력이 번거로워 금방 포기",
     "사진 한 장 · 문장 한 줄로 AI가 속성을 채워주는 디지털 옷장"),
    ("매일 아침 '오늘 뭐 입지?' 결정에 시간을 쓴다",
     "출근 · 약속 등 상황마다 조합을 고민 → 결정 피로, 결국 늘 입던 조합만 반복",
     "잠금 뽑기 '오늘의 픽' + 상황을 입력하면 추천하는 AI 코디"),
    ("한 주 일정에 맞춘 착용 계획이 없다",
     "요일별 일정을 고려하지 않고 당일에 고르다 보니 같은 옷이 연달아 겹치거나 세탁 · 준비가 늦어짐",
     "날짜별 코디 배치 주간 플래너 + AI 주간 추천"),
    ("내 코디 취향을 스스로 잘 모른다",
     "무엇이 어울리는지 기준이 없어 새로운 조합 시도가 어려움",
     "좌/우 선택 게임 '코디 챌린지'와 AI 평가 점수 · 스타일 태그"),
]
cards = "".join(
    f'<div style="display:flex; align-items:stretch; gap:14px;">'
    f'<div class="card" style="flex:1.25; background:#fff;"><p style="margin:0; font-size:12px; font-weight:800; color:#E03131;">PAIN POINT {i+1}</p>'
    f'<p style="margin:4px 0 4px; font-size:17px; font-weight:800;">{esc(t)}</p><p style="margin:0; font-size:13px; color:#555; line-height:1.5;">{esc(d)}</p></div>'
    f'<div style="align-self:center; font-size:26px; font-weight:800;">→</div>'
    f'<div class="card" style="flex:1; background:#1D2B50; color:#fff; border-color:#1D2B50; display:flex; flex-direction:column; justify-content:center;"><p style="margin:0; font-size:12px; font-weight:800; color:#A9C0EA;">SOLUTION {i+1}</p>'
    f'<p style="margin:4px 0 0; font-size:16px; font-weight:700; line-height:1.45;">{esc(s)}</p></div></div>'
    for i, (t, d, s) in enumerate(pains)
)
slide("01", "서비스 개요 – 기획 배경 및 Pain Point",
      "\"옷은 많은데 입을 옷이 없다\" – 보유 의류 파악 · 매일의 코디 결정 · 주간 계획의 불편을 해결",
      f'<div style="display:flex; flex-direction:column; gap:12px;">{cards}</div>')

features = [
    ("CLOSET", "옷장", "사진 AI 분석 · 문장 AI 일괄 등록 · 직접 입력, 카테고리/계절/색상 필터, 의류 상세 · 삭제"),
    ("HOME", "오늘의 픽", "슬롯별 랜덤 뽑기 + 잠금으로 원하는 옷 고정, '이걸로 입을래요'로 오늘 코디 확정"),
    ("OUTFIT", "코디", "상황 기반 AI 코디 추천 · 직접 만들기(순서 지정) 저장, 코디 상세 · 삭제"),
    ("PLANNER", "주간 플래너", "날짜별 코디 배치 · 교체 · 해제, AI 주간(1~7일) 추천 일괄 저장"),
    ("CHALLENGE", "코디 챌린지", "카테고리별 좌/우 선택 게임 → AI 점수 · 코멘트 · 태그 평가 후 코디 저장"),
]
fcards = "".join(
    f'<div class="card" style="flex:1; background:#fff;"><p style="margin:0; font-size:11px; font-weight:800; letter-spacing:0.08em; color:#999;">{a}</p>'
    f'<p style="margin:4px 0 8px; font-size:18px; font-weight:800;">{b}</p><p style="margin:0; font-size:13px; line-height:1.55; color:#444;">{esc(c)}</p></div>'
    for a, b, c in features
)
values = [
    ("빠른 옷장 구축", "AI가 사진·문장에서 속성을 추출하고, 사용자는 확인·수정만 한다"),
    ("결정 피로 감소", "보유 의류 안에서 뽑기 · 상황 추천으로 매일의 선택을 몇 초로 줄인다"),
    ("계획적인 착용", "한 주 코디를 미리 배치해 겹침 없이 준비한다"),
]
vcards = "".join(
    f'<div style="flex:1; border-left:4px solid #1D2B50; padding:4px 0 4px 14px;"><p style="margin:0; font-size:18px; font-weight:800;">{a}</p><p style="margin:5px 0 0; font-size:13px; color:#555; line-height:1.5;">{b}</p></div>'
    for a, b in values
)
slide("01", "서비스 개요 – 목적 · 핵심 가치 · 주요 기능", "",
      f'''<div style="display:flex; gap:24px; margin-top:6px;">
      <div class="card" style="flex:1.1;"><p style="margin:0; font-size:12px; font-weight:800; color:#999;">서비스 이름</p><p style="margin:4px 0 12px; font-size:24px; font-weight:800;">오늘 뭐 입지 <span class="muted" style="font-size:15px; font-weight:600;">Today Outfit</span></p>
      <p style="margin:0; font-size:12px; font-weight:800; color:#999;">서비스 목적</p><p style="margin:4px 0 10px; font-size:15px; line-height:1.6;">내가 <b>이미 가진 옷</b>을 AI로 손쉽게 디지털 옷장에 등록하고, 오늘의 코디부터 한 주의 코디까지 <b>보유 의류 안에서</b> 추천 · 계획할 수 있게 한다.</p>
      <p style="margin:0; font-size:12px; font-weight:800; color:#999;">타겟 사용자</p><p style="margin:4px 0 0; font-size:14px; line-height:1.55;">옷은 있지만 매일 코디 결정에 시간을 쓰는 <b>20~30대 직장인 · 학생</b> (스마트폰으로 옷을 촬영 · 관리하는 개인 사용자)</p></div>
      <div class="card" style="flex:1; background:#fff;"><p style="margin:0 0 10px; font-size:12px; font-weight:800; color:#999;">서비스 범위 (실현 가능성)</p>
      <p style="margin:0 0 10px; font-size:13px; line-height:1.75;">✔ 개인 사용자의 보유 의류 기반 기능만 제공 (Web · 모바일 브라우저)<br/>✔ AI는 외부 모델 API 호출, 결과는 <b>후보 → 사용자 확인 → 저장</b><br/>✘ 쇼핑 연동 · SNS 공유 · 날씨 연동은 이번 범위에서 제외</p>
      <p style="margin:0 0 4px; font-size:12px; font-weight:800; color:#999;">설계 제약 · 가정</p>
      <p style="margin:0; font-size:12.5px; line-height:1.65;">사진 JPG/PNG 5MB 이하 · 문장 등록 1회 최대 8벌 · AI 주간 추천 1~7일 · 하루 1코디 배치 · AI 일일 호출 한도 적용</p></div></div>
      <p style="margin:20px 0 10px; font-size:15px; font-weight:800;">핵심 가치</p><div style="display:flex; gap:26px;">{vcards}</div>
      <p style="margin:22px 0 10px; font-size:15px; font-weight:800;">주요 기능 (하단 탭 5개)</p><div style="display:flex; gap:12px;">{fcards}</div>''')

# ---------------------------------------------------------------- 02 액터
actors = [
    ("비회원 (방문자)", "서비스 진입 · 인증", "회원가입(이메일 · 비밀번호 · 이름), 로그인, 데모 계정으로 체험하기<br/>로그인 없이 보호 화면 접근 시 로그인 화면으로 이동"),
    ("회원 (사용자)", "옷장 · 코디 · 플래너 관리", "옷장 등록(사진 AI / 문장 AI / 직접) · 조회 · 필터 · 삭제, 오늘의 픽 뽑기 · 확정<br/>AI 코디 추천 · 직접 만들기 · 저장 · 삭제, 주간 플래너 배치 · 교체 · 해제 · AI 주간 추천<br/>코디 챌린지 · AI 평가 저장, 설정(로그아웃 · 계정 탈퇴) — <b>본인 데이터만 접근</b>"),
    ("데모 사용자", "빠른 기능 체험", "회원과 동일한 기능 사용. 샘플 의류 14벌 · 코디 2개 · 플래너 1건이 미리 채워진 체험 계정 (users.is_demo)"),
    ("AI 모델 (외부 시스템)", "분석 · 추천 · 평가", "의류 사진 인식(이름/카테고리/색상/계절), 의류 문장 파싱, 상황 기반 코디 추천, 주간 코디 추천, 챌린지 코디 평가<br/>서버가 호출하며 결과는 저장하지 않고 후보로만 반환 (호출 이력은 ai_requests에 기록)"),
    ("파일 저장소 (외부 시스템)", "의류 사진 보관", "업로드 사진 저장 및 image_url 제공, 저장되지 않은 임시 사진 정리, 의류 삭제 · 탈퇴 시 사진 삭제"),
]
arows = "".join(
    f'<tr><td style="font-weight:800; font-size:15px; width:210px; padding:13px 12px;">{a}</td><td style="width:190px; padding:13px 12px; font-weight:600;">{b}</td><td style="padding:13px 12px; font-size:13.5px;">{c}</td></tr>'
    for a, b, c in actors
)
slide("02", "시스템 액터 정의", "사용자 액터 3종(비회원 · 회원 · 데모 사용자)과 서버가 연동하는 외부 시스템 2종",
      f'<table><tr><th>Actor 구분</th><th>역할</th><th>상세 제공 기능</th></tr>{arows}</table>')

# ---------------------------------------------------------------- 03 요구사항
reqs = [
    ("FR-01", "비회원", "회원가입", "이메일 · 비밀번호 · 비밀번호 확인 · 이름 입력, 비밀번호 불일치 · 중복 이메일 검증", "S02", "POST /auth/signup"),
    ("FR-02", "비회원", "로그인 / 데모 체험", "이메일 · 비밀번호 로그인(JWT 발급), 데모 계정 자동 생성 후 로그인", "S01", "POST /auth/login · /auth/demo"),
    ("FR-03", "회원", "로그아웃 · 계정 탈퇴", "내 정보 표시, 비밀번호 재확인 후 전체 데이터 삭제(복구 불가 안내)", "S16", "GET /users/me · POST /auth/logout · POST /users/me/withdrawal"),
    ("FR-04", "회원", "홈 상태 표시", "보유 의류 수 · 오늘 배치 여부에 따라 빈 상태 / 뽑기 / 확정됨 분기", "S03", "GET /home/summary"),
    ("FR-05", "회원", "오늘의 픽", "상의 · 하의 · 신발 · 액세서리 랜덤 뽑기, 잠금 후 다시 돌리기, 오늘 코디로 확정", "S03", "POST /home/today-pick · POST /outfits · PUT /planner/schedules/{date}"),
    ("FR-06", "회원", "옷장 목록 · 필터", "카테고리 · 계절 · 색상 조합 필터, 필터 초기화, 등록 방법 선택 시트", "S04", "GET /clothes"),
    ("FR-07", "회원·AI", "사진으로 등록", "JPG/PNG 5MB 이하 업로드 → AI 속성 추정 → 확인 · 수정 후 저장, 실패 시 수동 등록 유도", "S05", "POST /ai/clothes/photo-analysis · POST /clothes"),
    ("FR-08", "회원·AI", "문장으로 일괄 등록", "문장 AI 파싱 → 후보 최대 8건 수정 · 삭제 → 일괄 저장", "S06", "POST /ai/clothes/text-parse · POST /clothes/bulk"),
    ("FR-09", "회원", "직접 입력 등록", "사진(선택) · 이름 · 카테고리 · 색상 · 계절 입력", "S07", "POST /images · POST /clothes"),
    ("FR-10", "회원", "의류 상세 · 삭제", "속성 · 등록 경로 · 파일명 · 등록일 표시, 삭제", "S08", "GET · DELETE /clothes/{id}"),
    ("FR-11", "회원", "코디 목록", "저장 코디 최신순, 생성 경로 칩 · AI 점수 표시", "S09", "GET /outfits"),
    ("FR-12", "회원·AI", "AI 코디 추천", "상황 입력 → 슬롯별 추천 · 추천 이유 → 이름 · 메모 입력 후 저장", "S10", "POST /ai/outfits/recommend · POST /outfits"),
    ("FR-13", "회원", "직접 코디 만들기", "옷장에서 선택, 순서 변경(↑↓) · 제외, 이름 · 메모 입력 후 저장", "S11", "GET /clothes · POST /outfits"),
    ("FR-14", "회원", "코디 상세 · 삭제", "상황 · AI 이유 · 평가 · 태그 · 메모 · 구성 의류, 플래너 배치 모드 진입, 삭제", "S12", "GET · DELETE /outfits/{id}"),
    ("FR-15", "회원", "주간 플래너", "주 단위 조회 · 이동, 날짜별 코디 배치 · 교체 · 해제(하루 1코디)", "S13", "GET /planner/schedules · PUT · DELETE /planner/schedules/{date}"),
    ("FR-16", "회원·AI", "AI 주간 추천", "상황 · 시작일 · 일수(1~7) → 날짜별 추천 미리보기 → 플래너 전체 저장", "S13", "POST /ai/planner/weekly-recommend · POST /planner/weekly-outfits"),
    ("FR-17", "회원", "코디 챌린지", "카테고리별(2벌 이상) 좌/우 선택 라운드, 가리고 고르기", "S14", "GET /challenge/rounds"),
    ("FR-18", "회원·AI", "챌린지 AI 평가", "선택 조합 점수 · 코멘트 · 태그 평가, 실패 시 점수 없이 저장, 다시 도전", "S15", "POST /ai/outfits/evaluate · POST /outfits"),
]


def req_table(items):
    cell = "padding:6px 8px; font-size:12.5px;"
    r = "".join(
        f'<tr><td class="mono" style="{cell} font-weight:700; width:64px; white-space:nowrap;">{a}</td><td style="{cell} width:66px; white-space:nowrap;">{b}</td><td style="{cell} font-weight:800; width:132px;">{c}</td>'
        f'<td style="{cell}">{esc(d)}</td><td class="mono" style="{cell} width:48px; font-weight:700; white-space:nowrap;">{e}</td><td class="mono" style="{cell} width:330px; font-size:11px; color:#2B4C8C;">{esc(f)}</td></tr>'
        for a, b, c, d, e, f in items
    )
    return f'<table><tr><th>ID</th><th>액터</th><th>기능</th><th>상세 요구사항</th><th>화면</th><th>API</th></tr>{r}</table>'


slide("03", "요구사항 정의 (1/2) – 인증 · 홈 · 옷장", "서비스 개요의 Pain Point ↔ 기능 ↔ 화면(S-ID) ↔ API가 1:1로 연결되도록 정의", req_table(reqs[:10]))
slide("03", "요구사항 정의 (2/2) – 코디 · 플래너 · 챌린지", "화면 ID(S01~S16)는 04. UI 흐름도의 화면 캡처와 동일", req_table(reqs[10:]))


# ---------------------------------------------------------------- 04 전체 UI 흐름도
def box(sid, name, dark=False, small=""):
    bg = "#1D2B50" if dark else "#fff"
    fg = "#fff" if dark else "#1D2B50"
    sm = f'<div style="font-size:10.5px; color:{"#bbb" if dark else "#888"}; margin-top:2px;">{small}</div>' if small else ""
    return (f'<div style="border:1.5px solid #1D2B50; background:{bg}; color:{fg}; border-radius:8px; padding:7px 8px; text-align:center;">'
            f'<div style="font-size:10px; font-weight:800; opacity:.7;" class="mono">{sid}</div><div style="font-size:13.5px; font-weight:800;">{name}</div>{sm}</div>')


down = '<div style="text-align:center; color:#2B4C8C; font-weight:800; line-height:1; margin:3px 0;">↓</div>'
cols = [
    ("HOME", [box("S03", "홈", True, "오늘의 픽 · 확정"), down, box("S12", "코디 상세", small="상세 보기")]),
    ("CLOSET", [box("S04", "내 옷장", True, "필터 · 등록 시트"), down,
                '<div style="display:flex; flex-direction:column; gap:6px;">' + box("S05", "사진으로 등록", small="AI 분석") + box("S06", "문장으로 등록", small="AI 파싱") + box("S07", "직접 입력 등록") + "</div>",
                down, box("S08", "의류 상세", small="삭제")]),
    ("OUTFIT", [box("S09", "내 코디", True), down,
                '<div style="display:flex; flex-direction:column; gap:6px;">' + box("S10", "AI 코디 추천") + box("S11", "직접 코디 만들기") + "</div>",
                down, box("S12", "코디 상세", small="삭제 · 플래너 배치")]),
    ("PLANNER", [box("S13", "주간 플래너", True, "배치 · 교체 · 해제"), down,
                 '<div style="display:flex; flex-direction:column; gap:6px;">' + box("M", "코디 선택 시트") + box("M", "배치 변경 시트") + box("M", "AI 주간 추천 패널") + "</div>"]),
    ("CHALLENGE", [box("S14", "코디 챌린지", True, "좌/우 선택"), down, box("S15", "챌린지 결과", small="AI 평가 · 저장"), down, box("S12", "코디 상세")]),
]
colhtml = "".join(
    f'<div style="flex:1; background:#f5f5f5; border-radius:10px; padding:10px;"><div style="font-size:11px; font-weight:800; letter-spacing:.1em; color:#888; text-align:center; margin-bottom:8px;">{t}</div>{"".join(items)}</div>'
    for t, items in cols
)
flow_body = f'''
<div style="display:flex; gap:18px; align-items:stretch;">
  <div style="width:170px; display:flex; flex-direction:column; justify-content:center; gap:8px;">
    <div style="font-size:11px; font-weight:800; letter-spacing:.1em; color:#888; text-align:center;">AUTH (비로그인)</div>
    {box("S01", "로그인", True, "데모 체험")}
    <div style="text-align:center; color:#2B4C8C; font-weight:800;">⇅</div>
    {box("S02", "회원가입")}
    <div style="text-align:center; color:#2B4C8C; font-weight:800; font-size:20px; margin-top:8px;">↓ 로그인 성공</div>
  </div>
  <div style="flex:1;">
    <div style="display:flex; justify-content:space-between; align-items:center; background:#1D2B50; color:#fff; border-radius:8px; padding:8px 14px; margin-bottom:10px; font-size:13px;">
      <span><b>하단 탭 네비게이션</b> — HOME · CLOSET · OUTFIT · PLANNER · CHALLENGE (로그인 후 모든 화면 공통)</span>
      <span>상단 ⚙ → <b>S16 설정</b> (로그아웃 · 계정 탈퇴 → S01)</span>
    </div>
    <div style="display:flex; gap:10px;">{colhtml}</div>
  </div>
</div>
<div style="display:flex; gap:12px; margin-top:14px; font-size:12.5px;">
  <div class="note ui" style="flex:1;"><span class="tag t-ui">UI 이동</span>S12 코디 상세 "플래너에 배치하기" → S13 배치 모드 → 날짜 선택</div>
  <div class="note ui" style="flex:1;"><span class="tag t-ui">UI 이동</span>AI 실패 시 S05 · S06 → S07 직접 입력으로 이동</div>
  <div class="note err" style="flex:1;"><span class="tag t-err">예외</span>비로그인 접근 → S01 리다이렉트 / 없는 경로 → 404 화면</div>
</div>'''
slide("04", "UI 흐름도 – 전체 화면 구조", "총 16개 화면(S01~S16) + 바텀시트 모달 3종 · 모바일 퍼스트(최대 폭 460px) 웹", flow_body)

# ---------------------------------------------------------------- 04 화면별 UI 흐름
KIND = {"ui": ("t-ui", "UI 이동"), "api": ("t-api", "API"), "err": ("t-err", "예외"), "modal": ("t-err", "모달")}


def ui_slide(title, sub, shots, notes):
    imgs = []
    for i, (fname, cap, mapping) in enumerate(shots):
        if i:
            imgs.append('<div class="arrow">▶</div>')
        ov = overlay(fname, mapping)
        imgs.append(
            f'<div class="shot"><div class="shot-wrap"><img src="{(SHOTS / (fname + ".png")).as_uri()}"/>{ov}</div>'
            f'<div class="cap">{esc(cap)}</div></div>'
        )
    nhtml = ""
    for i, (k, t) in enumerate(notes, start=1):
        cls = "err" if k == "modal" else k
        nhtml += (f'<div class="note {cls}"><span class="nnum">{i}</span>'
                  f'<span><span class="tag {KIND[k][0]}">{KIND[k][1]}</span>{rich(t)}</span></div>')
    slide("04", title, sub, f'<div class="flow"><div class="shots">{"".join(imgs)}</div><div class="notes">{nhtml}</div></div>', legend=True)


ui_slide("UI 흐름 (공통) – 로그인 / 회원가입", "S01 로그인 · S02 회원가입",
         [("01_login", "S01 로그인", {"login": 1, "demo": 3}),
          ("01b_login_error", "S01 로그인 실패", {"login": 1, "error": 4}),
          ("02_signup_error", "S02 회원가입 검증", {"submit": 5, "error": 6})],
         [("api", "`POST /auth/login` 이메일 · 비밀번호 확인 → access_token + user 반환"),
          ("ui", "로그인 성공 → S03 홈 (보호 화면에서 튕겨온 경우 원래 화면으로 복귀)"),
          ("api", "`POST /auth/demo` 데모 체험 — 계정이 없으면 샘플 의류 14벌 · 코디 2개 · 플래너 1건 생성"),
          ("err", "401 → \"이메일 또는 비밀번호가 올바르지 않습니다.\" 폼 하단 표시"),
          ("api", "`POST /auth/signup` 이메일(소문자 정규화) · 비밀번호 · 비밀번호 확인 · 이름"),
          ("err", "400 PASSWORD_MISMATCH \"비밀번호가 일치하지 않습니다.\" / 409 EMAIL_DUPLICATED"),
          ("ui", "가입 완료 토스트 → S01 로그인 이동, 로그인 상태에서 S01 · S02 접근 시 S03")])

ui_slide("UI 흐름 (홈) – 오늘의 픽", "S03 홈 — 뽑기 전 → 뽑기 결과(잠금) → 오늘의 코디 확정",
         [("03_home_empty_pick", "S03 뽑기 전", {"count": 1, "draw": 2}),
          ("04_home_drawn", "S03 뽑기 · 잠금", {"lock": 3, "reroll": 2, "confirm": 4}),
          ("05_home_confirmed", "S03 확정됨", {"chip": 1, "detail": 5, "change": 5})],
         [("api", "`GET /home/summary` clothes_count · today_schedule로 3가지 상태 분기"),
          ("api", "`POST /home/today-pick` 슬롯별(상의 · 하의 · 신발 · 액세서리) 랜덤, locked_clothing_ids 유지"),
          ("ui", "🔒 잠금 버튼으로 마음에 드는 옷 고정 → \"다시 돌리기 (잠금 제외)\""),
          ("api", "\"이걸로 입을래요\" → `POST /outfits` (source=RANDOM) → `PUT /planner/schedules/{오늘}`"),
          ("ui", "\"상세 보기\" → S12 코디 상세 / \"다른 코디로 바꾸기\" → 뽑기 재시작"),
          ("err", "카테고리 옷이 없으면 \"OO 없음\" 슬롯, 의류 0벌이면 빈 상태 + \"옷 추가하러 가기\" → S07"),
          ("ui", "상단 ⚙ → S16 설정, 하단 탭으로 CLOSET · OUTFIT · PLANNER · CHALLENGE 이동")])

ui_slide("UI 흐름 (옷장) – 목록 · 필터 · 등록 방법 선택", "S04 내 옷장",
         [("06_wardrobe", "S04 내 옷장", {"filters": 1, "card": 2, "fab": 5}),
          ("06b_wardrobe_filter", "S04 필터 적용", {"filters": 1, "reset": 3}),
          ("07_wardrobe_add_sheet", "S04 등록 방법 시트", {"photo": 5, "text": 5, "manual": 5})],
         [("api", "`GET /clothes?category&season&color` 조건 조합 필터 · 최신 등록순 · 페이지네이션"),
          ("ui", "의류 카드 클릭 → S08 의류 상세"),
          ("ui", "필터를 하나라도 선택하면 \"필터 초기화\" 버튼 노출"),
          ("err", "결과 0건 \"조건에 맞는 옷이 없어요\" / 전체 0벌 \"아직 등록된 옷이 없어요\""),
          ("modal", "+ 버튼 → 바텀시트: 사진으로 등록(S05) · 문장으로 여러 벌 등록(S06) · 직접 입력(S07)"),
          ("ui", "사진이 없는 의류(image_url = null)는 카테고리 + 색상 대표 사진으로 표시")])

ui_slide("UI 흐름 (옷장) – 사진으로 등록 (AI 분석)", "S05 사진으로 등록 — 성공 / 실패 시나리오",
         [("08a_add_photo_empty", "S05 사진 선택", {"pick": 1, "limit": 4}),
          ("08b_add_photo_result", "S05 AI 결과 확인 · 수정", {"result": 2, "save": 3}),
          ("09_add_photo_fail", "S05 AI 분석 실패", {"error": 5, "manual": 5})],
         [("api", "`POST /ai/clothes/photo-analysis` (multipart) 업로드 + AI 비전 분석 → image_url · 속성 후보"),
          ("ui", "분석 중 로딩 표시, 결과 후보를 사용자가 수정 후 \"이 정보로 저장\""),
          ("api", "`POST /clothes` (source=PHOTO, image_url 포함) → 201 → S04 옷장 + 토스트"),
          ("err", "400 INVALID_FILE \"5MB 이하의 JPG 또는 PNG 파일만 업로드할 수 있어요.\""),
          ("err", "502 AI_ANALYSIS_FAILED → \"수동 등록으로 이동\" 버튼 → S07 직접 입력"),
          ("err", "429 AI_RATE_LIMITED 일일 AI 호출 한도 초과 안내")])

ui_slide("UI 흐름 (옷장) – 문장 등록 · 직접 입력 · 의류 상세", "S06 문장으로 등록 · S07 직접 입력 등록 · S08 의류 상세",
         [("10_add_text_candidates", "S06 문장 → 후보", {"analyze": 1, "cand": 1, "save": 2}),
          ("11_add_manual", "S07 직접 입력", {"photo": 4, "save": 4}),
          ("12_clothing_detail", "S08 의류 상세", {"meta": 5, "del": 6})],
         [("api", "`POST /ai/clothes/text-parse` 문장 분해 → 후보 최대 8건 (수정 / 삭제 가능)"),
          ("api", "`POST /clothes/bulk` (source=TEXT) \"N벌 일괄 저장\" — 한 트랜잭션"),
          ("err", "파싱 실패 502 → \"수동 등록으로 이동\" / 문장 미입력 시 분석 버튼 비활성"),
          ("api", "`POST /images` (선택 사진) → `POST /clothes` (source=MANUAL), 이름 미입력 시 버튼 비활성"),
          ("api", "`GET /clothes/{id}` 카테고리 · 색상 · 계절 칩, 등록 경로 · 파일명 · 등록일"),
          ("api", "`DELETE /clothes/{id}` → S04 (이 옷을 포함한 코디에서는 해당 옷만 제외)"),
          ("err", "404 \"삭제되었거나 존재하지 않는 의류입니다.\"")])

ui_slide("UI 흐름 (코디) – 목록 · AI 추천 · 상세", "S09 내 코디 · S10 AI 코디 추천 · S12 코디 상세",
         [("13_outfits", "S09 내 코디", {"card": 1, "ai": 2, "manual": 2}),
          ("14_outfit_ai_result", "S10 AI 코디 추천", {"situation": 3, "result": 3, "save": 4}),
          ("16_outfit_detail", "S12 코디 상세", {"reason": 6, "place": 7, "del": 7})],
         [("api", "`GET /outfits` 썸네일 3개 · 이름 · 생성 경로 칩 · AI 점수"),
          ("ui", "\"AI 추천\" → S10 / \"직접 만들기\" → S11 / 코디 카드 → S12"),
          ("api", "`POST /ai/outfits/recommend` {situation} → 슬롯별 의류 · 추천 이유 · 이름 기본값"),
          ("api", "`POST /outfits` (source=AI, request_text, ai_reason) → S12 이동"),
          ("err", "상황 미입력 400 / 보유 의류 0벌 422 NO_CLOTHES / 502 \"추천을 만드는 데 실패했어요\""),
          ("api", "`GET /outfits/{id}` 입력한 상황 · AI 추천 이유 · 코멘트 · #태그 · 메모 · 구성 의류"),
          ("ui", "\"플래너에 배치하기\" → S13 배치 모드 / `DELETE /outfits/{id}` → S09")])

ui_slide("UI 흐름 (코디) – 직접 만들기 · 플래너 배치 모드", "S11 직접 코디 만들기 · S12 → S13 배치 모드",
         [("15_outfit_manual", "S11 직접 코디 만들기", {"select": 1, "order": 1, "save": 2}),
          ("19b_planner_place_mode", "S13 배치 모드", {"banner": 4, "day": 5})],
         [("api", "`GET /clothes` 옷장에서 의류 선택(✓ 배지) — 선택 목록에서 ↑↓ 순서 변경 · ✕ 제외"),
          ("api", "`POST /outfits` (source=MANUAL) — clothing_ids 배열 순서가 outfit_items.item_order로 저장"),
          ("err", "선택 0벌 또는 이름 미입력 시 저장 버튼 비활성 / 타인 의류 ID 400 INVALID_CLOTHING"),
          ("ui", "S12 코디 상세 \"플래너에 배치하기\" → 상단 배너 \"'코디명' 코디를 배치할 날짜를 선택하세요\""),
          ("api", "날짜 클릭 → `PUT /planner/schedules/{date}` 신규 201 / 기존 200 \"코디를 교체했어요\""),
          ("ui", "배너 \"취소\" → 일반 플래너 모드로 복귀")])

ui_slide("UI 흐름 (플래너) – 주간 조회 · 배치 · 교체 · 해제", "S13 주간 플래너 + 바텀시트 모달",
         [("17_planner", "S13 주간 플래너", {"nav": 1, "empty": 2, "filled": 4}),
          ("18_planner_picker", "S13 코디 선택 시트", {"title": 2, "card": 3}),
          ("19_planner_action", "S13 배치 변경 시트", {"replace": 3, "unset": 5})],
         [("api", "`GET /planner/schedules?start_date&end_date` 월~일 7일, 이전 주 · 이번 주 · 다음 주 이동"),
          ("modal", "빈 날짜(+ 코디 배치하기) 클릭 → 저장 코디 선택 시트 (`GET /outfits`)"),
          ("api", "코디 선택 → `PUT /planner/schedules/{date}` → \"코디를 배치했어요\""),
          ("modal", "배치된 날짜 클릭 → 상세 보기(S12) · 다른 코디로 교체 · 배치 해제 · 닫기"),
          ("api", "배치 해제 → `DELETE /planner/schedules/{date}` → \"배치를 해제했어요\""),
          ("err", "저장 코디 0개면 \"저장된 코디가 없어요\" / 하루 1코디 (user_id, plan_date) 유니크")])

ui_slide("UI 흐름 (플래너) – AI 주간 코디 추천", "S13 주간 플래너 하단 AI 주간 추천 패널",
         [("20_planner_ai", "S13 AI 주간 추천 결과", {"request": 2, "days": 3, "save": 4})],
         [("ui", "\"AI 주간 코디 추천 열기\" → 상황 · 시작일 · 일수(1~7) 입력 패널 토글"),
          ("api", "`POST /ai/planner/weekly-recommend` → 날짜별 슬롯 · clothing_ids · ai_reason"),
          ("ui", "날짜별 추천 코디 썸네일을 저장 전에 미리보기"),
          ("api", "\"플래너에 전체 저장\" → `POST /planner/weekly-outfits` 코디 생성 + 배치를 한 트랜잭션으로 처리"),
          ("ui", "저장 완료 토스트 \"N일치 코디를 플래너에 저장했어요\" → 주간 목록 반영, 패널 닫힘"),
          ("err", "의류 부족 시 generated_days < requested_days → \"N일치만 생성했어요\""),
          ("err", "0일 생성 422 NO_CLOTHES / 상황 미입력 400 / 이미 배치된 날짜는 교체")])

ui_slide("UI 흐름 (챌린지) – 좌/우 선택 게임 · AI 평가", "S14 코디 챌린지 · S15 챌린지 결과",
         [("21_challenge_start", "S14 챌린지 시작", {"guide": 1, "start": 1}),
          ("22_challenge_round", "S14 좌/우 선택", {"left": 2, "blur": 2}),
          ("23_challenge_result", "S15 결과 · AI 평가", {"score": 4, "save": 5, "retry": 7})],
         [("api", "`GET /challenge/rounds` 상의 → 하의 → 신발 → 모자 → 액세서리, 2벌 이상 카테고리만 구성"),
          ("ui", "좌/우 카드 선택 → 다음 라운드 (\"가리고 고르기\" 체크 시 사진 블러 · 이름 ???)"),
          ("ui", "마지막 라운드 선택 → S15 결과 화면"),
          ("api", "`POST /ai/outfits/evaluate` 점수(0~100) · 코멘트 · 스타일 태그 3개"),
          ("api", "\"코디로 저장하기\" → `POST /outfits` (source=CHALLENGE, ai_score · ai_comment · ai_tags) → S12"),
          ("err", "라운드 0개 422 NOT_ENOUGH_CLOTHES / 평가 실패 502 → 점수 없이 저장 가능"),
          ("ui", "\"다시 도전하기\" → S14, 선택 기록 없이 S15 접근 시 S14로 리다이렉트")])

ui_slide("UI 흐름 (설정) – 로그아웃 · 계정 탈퇴", "S16 설정 (상단 ⚙ 아이콘으로 진입)",
         [("24_settings_withdraw", "S16 계정 탈퇴 확인", {"warn": 3, "pw": 4, "submit": 4})],
         [("api", "`GET /users/me` 이름 · 이메일 표시"),
          ("api", "\"로그아웃\" → `POST /auth/logout` → 저장된 토큰 삭제 → S01 로그인"),
          ("modal", "\"계정 탈퇴\" → 확인 단계: 삭제 범위 및 복구 불가 안내 + 현재 비밀번호 입력"),
          ("api", "\"최종 탈퇴하기\" → `POST /users/me/withdrawal` → 204 → 하위 데이터 CASCADE 삭제 → S01"),
          ("err", "400 INVALID_PASSWORD \"현재 비밀번호가 올바르지 않습니다.\" / 미입력 시 버튼 비활성"),
          ("ui", "\"취소\" → 설정 첫 화면으로 복귀")])

# ---------------------------------------------------------------- 05 ERD
TABLES = {
    "users": (20, 30, [("id", "bigint", "PK"), ("email", "varchar(100)", "UQ NN"), ("password", "varchar(255)", "NN"), ("name", "varchar(30)", "NN"), ("is_demo", "boolean", "NN"), ("created_at", "timestamp", "NN"), ("updated_at", "timestamp", "")]),
    "ai_requests": (20, 250, [("id", "bigint", "PK"), ("user_id", "bigint", "FK NN"), ("type", "AiRequestType", "E NN"), ("status", "AiRequestStatus", "E NN"), ("latency_ms", "integer", ""), ("created_at", "timestamp", "NN")]),
    "clothes": (320, 30, [("id", "bigint", "PK"), ("user_id", "bigint", "FK NN"), ("name", "varchar(50)", "NN"), ("category", "ClothingCategory", "E NN"), ("color", "varchar(10)", "NN"), ("season", "SeasonType", "E NN"), ("image_url", "varchar(500)", ""), ("image_file_name", "varchar(255)", ""), ("source", "ClothingSource", "E NN"), ("created_at", "timestamp", "NN"), ("updated_at", "timestamp", "")]),
    "outfit_items": (620, 30, [("id", "bigint", "PK"), ("outfit_id", "bigint", "FK NN"), ("clothing_id", "bigint", "FK NN"), ("item_order", "integer", "NN")]),
    "schedules": (620, 180, [("id", "bigint", "PK"), ("user_id", "bigint", "FK NN"), ("plan_date", "date", "NN"), ("outfit_id", "bigint", "FK NN"), ("created_at", "timestamp", "NN"), ("updated_at", "timestamp", "")]),
    "outfits": (920, 30, [("id", "bigint", "PK"), ("user_id", "bigint", "FK NN"), ("name", "varchar(50)", "NN"), ("memo", "varchar(500)", ""), ("source", "OutfitSource", "E NN"), ("request_text", "varchar(500)", ""), ("ai_reason", "text", ""), ("ai_score", "integer", ""), ("ai_comment", "varchar(500)", ""), ("created_at", "timestamp", "NN"), ("updated_at", "timestamp", "")]),
    "outfit_ai_tags": (920, 300, [("id", "bigint", "PK"), ("outfit_id", "bigint", "FK NN"), ("tag", "varchar(30)", "NN")]),
}
TW, HH, RH = 250, 26, 18


def rowy(t, col):
    x, y, cols = TABLES[t]
    i = [c[0] for c in cols].index(col)
    return y + HH + i * RH + RH / 2


def erd_svg():
    parts = []

    def poly(pts):
        d = " ".join(f"{x},{y}" for x, y in pts)
        parts.append(f'<polyline points="{d}" fill="none" stroke="#9DB0D8" stroke-width="1.4"/>')
        (x0, y0), (x1, y1) = pts[0], pts[-1]
        fx = x0 + (-9 if pts[1][0] < x0 else 5)
        px = x1 + (-9 if pts[-2][0] < x1 else 5)
        parts.append(f'<text x="{fx}" y="{y0 - 3}" font-size="10" font-weight="800" fill="#E03131">N</text>')
        parts.append(f'<text x="{px}" y="{y1 - 3}" font-size="10" font-weight="800" fill="#1D2B50">1</text>')

    uid = rowy("users", "id")
    y = rowy("clothes", "user_id"); poly([(320, y), (292, y), (292, uid), (270, uid)])
    y = rowy("ai_requests", "user_id"); poly([(270, y), (284, y), (284, uid + 6), (270, uid + 6)])
    y = rowy("outfits", "user_id"); poly([(920, y), (902, y), (902, 12), (300, 12), (300, uid - 6), (270, uid - 6)])
    y = rowy("schedules", "user_id"); poly([(620, y), (606, y), (606, 20), (306, 20), (306, uid), (292, uid)])
    oid = rowy("outfits", "id")
    cid = rowy("clothes", "id")
    y = rowy("outfit_items", "outfit_id"); poly([(870, y), (884, y), (884, oid), (920, oid)])
    y = rowy("outfit_items", "clothing_id"); poly([(620, y), (592, y), (592, cid), (570, cid)])
    y = rowy("schedules", "outfit_id"); poly([(870, y), (892, y), (892, oid + 6), (920, oid + 6)])
    y = rowy("outfit_ai_tags", "outfit_id"); poly([(920, y), (910, y), (910, oid - 6), (920, oid - 6)])

    for name, (x, y, cols) in TABLES.items():
        h = HH + len(cols) * RH
        parts.append(f'<rect x="{x}" y="{y}" width="{TW}" height="{h}" rx="5" fill="#fff" stroke="#1D2B50" stroke-width="1.2"/>')
        parts.append(f'<rect x="{x}" y="{y}" width="{TW}" height="{HH}" rx="5" fill="#1D2B50"/>')
        parts.append(f'<rect x="{x}" y="{y + HH - 6}" width="{TW}" height="6" fill="#1D2B50"/>')
        parts.append(f'<text x="{x + 10}" y="{y + 17}" font-size="13" font-weight="800" fill="#fff">{name}</text>')
        for i, (c, t, k) in enumerate(cols):
            ry = y + HH + i * RH
            if i % 2:
                parts.append(f'<rect x="{x + 1}" y="{ry}" width="{TW - 2}" height="{RH}" fill="#f6f6f6"/>')
            color = "#2B4C8C" if "PK" in k else ("#6E86BE" if "FK" in k else "#1D2B50")
            weight = 800 if ("PK" in k or "FK" in k) else 500
            parts.append(f'<text x="{x + 10}" y="{ry + 13}" font-size="11.5" font-weight="{weight}" fill="{color}">{c}</text>')
            parts.append(f'<text x="{x + 118}" y="{ry + 13}" font-size="10.5" fill="#777" font-family="Menlo, monospace">{t}</text>')
            parts.append(f'<text x="{x + TW - 8}" y="{ry + 13}" font-size="9.5" font-weight="800" fill="#999" text-anchor="end">{k}</text>')
    return f'<svg width="1180" height="390" viewBox="0 0 1180 390" xmlns="http://www.w3.org/2000/svg" style="font-family:Apple SD Gothic Neo, sans-serif">{"".join(parts)}</svg>'


erd_body = f'''
<div style="border:1px solid #eee; border-radius:10px; padding:6px 0 0; background:#fcfcfc;">{erd_svg()}</div>
<div style="display:flex; gap:10px; margin-top:12px; font-size:12.5px;">
  <div class="note" style="flex:1;"><b>users 1:N</b> clothes · outfits · schedules · ai_requests (탈퇴 시 CASCADE 삭제)</div>
  <div class="note" style="flex:1;"><b>outfits M:N clothes</b> → <span class="mono">outfit_items</span>로 해소, item_order로 표시 순서 저장</div>
  <div class="note" style="flex:1;"><b>schedules</b> (user_id, plan_date) UNIQUE → 하루 1코디, PUT 업서트 기준</div>
  <div class="note" style="flex:1;"><span style="color:#2B4C8C; font-weight:800;">PK</span> · <span style="color:#6E86BE; font-weight:800;">FK</span> · E=Enum · NN=Not Null · UQ=Unique</div>
</div>'''
slide("05", "데이터 모델 설계 – ERD", "7개 엔티티 · 6개 Enum · 파일: 8반_김민_오늘뭐입지-DB.dbml (dbdiagram.io)", erd_body)

points = [
    ("M:N 관계의 명확한 해소", "코디 ↔ 의류를 outfit_items로 분리하고 (outfit_id, clothing_id) · (outfit_id, item_order) UNIQUE로 중복 · 순서 충돌 방지"),
    ("하루 1코디 업서트", "schedules (user_id, plan_date) UNIQUE → 같은 날짜 PUT은 신규 201 / 교체 200으로 자연스럽게 처리"),
    ("Enum으로 상태값 무결성", "category · season · source(의류/코디) · AI 요청 유형/상태를 Enum으로 제한, 색상은 허용값 7종을 API enum으로 검증"),
    ("AI 결과 정규화", "챌린지 스타일 태그는 배열 컬럼 대신 outfit_ai_tags(1:N)로 분리, 추천 이유 · 점수 · 코멘트는 source별 선택 컬럼"),
    ("삭제 정책(CASCADE)", "의류 삭제 → 코디 구성에서만 제외 / 코디 삭제 → 배치 해제 / 탈퇴 → 모든 개인 데이터 삭제"),
    ("AI 호출 이력", "ai_requests에 호출 메타데이터만 기록 → 일일 호출 한도(429) · 실패율 · 지연 모니터링"),
]
prow = "".join(
    f'<div style="display:flex; gap:10px; padding:9px 0; border-bottom:1px solid #eee;"><span style="font-weight:800; font-size:18px; width:26px;">{i+1}</span>'
    f'<div><p style="margin:0; font-size:14.5px; font-weight:800;">{a}</p><p style="margin:3px 0 0; font-size:12.5px; color:#555; line-height:1.5;">{esc(b)}</p></div></div>'
    for i, (a, b) in enumerate(points)
)
maps = [
    ("S04 · S08", "의류 카드 · 상세 (이름 · 카테고리 · 색상 · 계절 · 사진)", "clothes.name · category · color · season · image_url"),
    ("S08", "등록 경로 · 파일명 · 등록일", "clothes.source · image_file_name · created_at"),
    ("S09", "코디 카드 (이름 · 생성 경로 칩 · AI 점수 · 썸네일)", "outfits.name · source · ai_score, outfit_items"),
    ("S12", "입력한 상황 · AI 추천 이유 · 코멘트 · #태그 · 메모", "outfits.request_text · ai_reason · ai_comment · memo, outfit_ai_tags.tag"),
    ("S11", "선택 의류 ↑↓ 순서", "outfit_items.item_order"),
    ("S03 · S13", "요일 · 날짜별 코디 / 오늘의 코디", "schedules.plan_date · outfit_id"),
    ("S16", "이름 · 이메일 / 탈퇴 비밀번호 확인", "users.name · email · password"),
    ("S01", "데모 계정으로 체험하기", "users.is_demo"),
]
mrow = "".join(f'<tr><td class="mono" style="width:92px; font-weight:700; white-space:nowrap;">{a}</td><td>{esc(b)}</td><td class="mono" style="font-size:11.5px; color:#6E86BE;">{esc(c)}</td></tr>' for a, b, c in maps)
slide("05", "데이터 모델 설계 – 설계 포인트 · 화면 필드 매핑", "UI에서 사용하는 모든 데이터 필드가 ERD 컬럼에 존재하는지 화면 기준으로 검증",
      f'<div style="display:flex; gap:30px;"><div style="flex:1;">{prow}</div><div style="flex:1.15;"><table><tr><th>화면</th><th>UI 데이터</th><th>엔티티 · 컬럼</th></tr>{mrow}</table></div></div>')

# ---------------------------------------------------------------- 06 API
apis = [
    ("Auth", "POST", "/auth/signup", "회원가입", "S02"),
    ("Auth", "POST", "/auth/login", "로그인 (JWT 발급)", "S01"),
    ("Auth", "POST", "/auth/demo", "데모 계정 체험 (샘플 데이터 생성)", "S01"),
    ("Auth", "POST", "/auth/logout", "로그아웃", "S16"),
    ("User", "GET", "/users/me", "내 정보 조회", "S16"),
    ("User", "POST", "/users/me/withdrawal", "계정 탈퇴 (비밀번호 재확인)", "S16"),
    ("Home", "GET", "/home/summary", "홈 요약 (의류 수 · 오늘 배치)", "S03"),
    ("Home", "POST", "/home/today-pick", "오늘의 픽 뽑기 (잠금 유지)", "S03"),
    ("Clothes", "GET", "/clothes", "옷장 목록 (카테고리 · 계절 · 색상 필터)", "S04 S11"),
    ("Clothes", "POST", "/clothes", "의류 등록 (사진 확인 / 직접 입력)", "S05 S07"),
    ("Clothes", "POST", "/clothes/bulk", "의류 일괄 등록 (문장)", "S06"),
    ("Clothes", "GET", "/clothes/{clothingId}", "의류 상세", "S08"),
    ("Clothes", "DELETE", "/clothes/{clothingId}", "의류 삭제", "S08"),
    ("Clothes", "POST", "/images", "의류 사진 업로드 (multipart)", "S07"),
    ("Outfits", "GET", "/outfits", "코디 목록", "S09 S13"),
    ("Outfits", "POST", "/outfits", "코디 저장 (AI · 직접 · 오늘의 픽 · 챌린지)", "S03 S10 S11 S15"),
    ("Outfits", "GET", "/outfits/{outfitId}", "코디 상세", "S12"),
    ("Outfits", "DELETE", "/outfits/{outfitId}", "코디 삭제 (배치 함께 해제)", "S12"),
    ("Planner", "GET", "/planner/schedules", "기간별 배치 조회", "S13"),
    ("Planner", "PUT", "/planner/schedules/{planDate}", "날짜 배치 · 교체 (업서트 201/200)", "S03 S13"),
    ("Planner", "DELETE", "/planner/schedules/{planDate}", "배치 해제", "S13"),
    ("Planner", "POST", "/planner/weekly-outfits", "AI 주간 추천 일괄 저장 (트랜잭션)", "S13"),
    ("Challenge", "GET", "/challenge/rounds", "챌린지 라운드 생성", "S14"),
    ("AI", "POST", "/ai/clothes/photo-analysis", "의류 사진 AI 분석 (multipart)", "S05"),
    ("AI", "POST", "/ai/clothes/text-parse", "의류 문장 AI 파싱", "S06"),
    ("AI", "POST", "/ai/outfits/recommend", "상황 기반 코디 추천", "S10"),
    ("AI", "POST", "/ai/outfits/evaluate", "챌린지 코디 평가", "S15"),
    ("AI", "POST", "/ai/planner/weekly-recommend", "주간 코디 추천", "S13"),
]


def api_table(items):
    cell = "padding:6px 10px;"
    r = "".join(
        f'<tr><td style="{cell} width:90px; font-weight:700;">{t}</td><td style="{cell} width:74px;"><span class="pill {m}">{m}</span></td>'
        f'<td class="mono" style="{cell} font-weight:700; width:330px; font-size:12px;">{esc(p)}</td><td style="{cell}">{esc(d)}</td>'
        f'<td class="mono" style="{cell} width:170px; font-size:11px; white-space:nowrap;">{s}</td></tr>'
        for t, m, p, d, s in items
    )
    return f'<table><tr><th>Tag</th><th>Method</th><th>Endpoint</th><th>설명</th><th>호출 화면</th></tr>{r}</table>'


slide("06", "API 명세 정의 (1/3) – Auth · User · Home · Clothes", "OpenAPI 3.0.3 · 28 operations · 파일: 8반_김민_오늘뭐입지-API.yml (스펙 검증 통과)", api_table(apis[:14]))
slide("06", "API 명세 정의 (2/3) – Outfits · Planner · Challenge · AI", "Base URL http://localhost:8080/api · JWT Bearer 인증(Security Schemes 정의 제외)", api_table(apis[14:]))

errs = [
    ("400", "INVALID_FILE · PASSWORD_MISMATCH · INVALID_PASSWORD · INVALID_CLOTHING", "입력 폼 하단 에러 문구 / 버튼 비활성"),
    ("401", "UNAUTHORIZED", "로그인 실패 문구, 토큰 만료 시 S01 리다이렉트"),
    ("404", "NOT_FOUND", "\"삭제되었거나 존재하지 않는 OO입니다.\" (타인 리소스도 404)"),
    ("409", "EMAIL_DUPLICATED", "\"이미 가입된 이메일입니다.\""),
    ("422", "NO_CLOTHES · NOT_ENOUGH_CLOTHES", "빈 상태 화면 + 옷 등록 유도"),
    ("429", "AI_RATE_LIMITED", "일일 AI 호출 한도 초과 안내"),
    ("502", "AI_ANALYSIS_FAILED", "재시도 / \"수동 등록으로 이동\" 버튼"),
]
erow = "".join(f'<tr><td class="mono" style="font-weight:800; width:52px;">{a}</td><td class="mono" style="font-size:11.5px; width:300px;">{b}</td><td>{esc(c)}</td></tr>' for a, b, c in errs)
schemas = [
    ("시스템 공통", "PagedResponse (content · total_elements · total_pages · page · size), Error (code · error_code · message)"),
    ("Enum", "ClothingCategory · SeasonType · ClothingColor · ClothingSource · OutfitSource"),
    ("도메인 재사용", "ClothingSummary · OutfitSummary · OutfitSlot → 코디 · 플래너 · 챌린지 · AI 응답에서 공통 사용"),
    ("공통 파라미터", "PageParam · SizeParam · ClothingIdPath · OutfitIdPath · PlanDatePath"),
    ("공통 응답", "BadRequest · Unauthorized · NotFound · Conflict · UnprocessableEntity · TooManyRequests · AiServiceError"),
]
srow = "".join(f'<div style="padding:8px 0; border-bottom:1px solid #eee;"><p style="margin:0; font-size:13.5px; font-weight:800;">{a}</p><p style="margin:3px 0 0; font-size:12.5px; color:#555; line-height:1.5;">{esc(b)}</p></div>' for a, b in schemas)
principles = [
    "AI API(<span class='mono'>/ai/**</span>)는 <b>후보만 반환</b>하고, 저장 API(<span class='mono'>POST /clothes · /outfits</span>)와 분리",
    "날짜 배치는 <span class='mono'>PUT /planner/schedules/{planDate}</span> 업서트 — 멱등성 보장",
    "여러 건 저장(<span class='mono'>/clothes/bulk · /planner/weekly-outfits</span>)은 단일 트랜잭션",
    "모든 리소스는 로그인 사용자 소유만 조회 · 수정, 타 사용자 리소스는 존재 노출 없이 404",
]
prin = "".join(f'<li style="margin-bottom:6px;">{p}</li>' for p in principles)
slide("06", "API 명세 정의 (3/3) – 공통 스키마 · 에러 코드 · 설계 원칙", "$ref 공통 스키마 재사용 · 상태 코드 세분화 · error_code로 화면 분기",
      f'''<div style="display:flex; gap:28px;"><div style="flex:1;">{srow}
      <p style="margin:14px 0 6px; font-size:14px; font-weight:800;">설계 원칙</p><ul style="margin:0; padding-left:18px; font-size:12.5px; line-height:1.5;">{prin}</ul></div>
      <div style="flex:1.1;"><table><tr><th>HTTP</th><th>error_code</th><th>화면 처리</th></tr>{erow}</table></div></div>''')

# ---------------------------------------------------------------- 07 고려사항
worries = [
    ("AI 결과를 바로 저장할 것인가?", "오인식 데이터가 옷장에 누적되지 않도록 '후보 반환 → 사용자 확인 · 수정 → 저장' 2단계로 분리. AI API는 상태를 갖지 않아 재시도가 안전"),
    ("오늘의 픽 확정 = 호출 2회", "코디 생성 후 배치가 실패하면 배치되지 않은 코디가 남음 → 단일 트랜잭션 API 통합 여부 검토"),
    ("삭제 범위를 어디까지?", "의류 삭제 시 코디 전체를 지우면 사용자 손실이 커서 구성에서만 제외, 반면 코디 삭제 시 배치는 의미가 없어 함께 해제"),
    ("색상 값 관리", "화면 표시값(한글 7종)을 enum으로 고정해 UI ↔ API ↔ DB를 일치시켰으나, 색상 추가 · 다국어 시 코드 테이블 분리 필요"),
]
nexts = [
    ("프론트엔드 연동", "현재 앱은 목업 AI + localStorage 프로토타입 → Pinia store 액션을 API 클라이언트(axios)로 교체, 로딩 · 에러 상태 공통화"),
    ("AI 연동 안정성", "응답 지연(수 초) 대비 타임아웃 · 재시도 · 로딩 UI, 일일 호출 한도(429) 및 비용 · 실패율 모니터링(ai_requests)"),
    ("이미지 처리", "오브젝트 스토리지 업로드, 리사이즈 · 썸네일 생성, 저장되지 않은 임시 사진 24시간 정리 배치"),
    ("보안", "BCrypt 비밀번호, JWT 만료 · Refresh 토큰, 모든 API 소유권 검증, 업로드 파일 MIME · 용량 서버 검증"),
    ("테스트", "AI 모델 Mock 기반 API 통합 테스트, 업서트 201/200 · 일괄 저장 롤백 · CASCADE 삭제 케이스 검증"),
]


def col(title, items, dark):
    rows_ = "".join(
        f'<div style="padding:9px 0; border-bottom:1px solid {"#333" if dark else "#eee"};"><p style="margin:0; font-size:14.5px; font-weight:800;">{esc(a)}</p>'
        f'<p style="margin:3px 0 0; font-size:12.5px; line-height:1.55; color:{"#ccc" if dark else "#555"};">{esc(b)}</p></div>'
        for a, b in items
    )
    style = "background:#1D2B50; color:#fff; border-color:#1D2B50;" if dark else "background:#fff;"
    return f'<div class="card" style="flex:1; {style}"><p style="margin:0 0 4px; font-size:17px; font-weight:800;">{title}</p>{rows_}</div>'


slide("07", "고려사항", "설계 과정에서의 고민과 다음 단계(애플리케이션 개발 · 테스트)에서 반영할 사항",
      f'<div style="display:flex; gap:20px;">{col("설계 상의 고민", worries, False)}{col("다음 단계 개발 시 고려사항", nexts, True)}</div>')

OUT.write_text(f'<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>오늘 뭐 입지 개요</title><style>{CSS}</style></head><body>{"".join(pages)}</body></html>', encoding="utf-8")
print("deck pages:", len(pages))

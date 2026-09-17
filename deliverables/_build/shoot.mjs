// Headless Chrome(CDP)로 today-outfit-app 화면을 조작하며 캡처한다.
// 각 화면에서 spec.mjs에 정의한 강조 영역의 좌표(뷰포트 기준 CSS px)를 annots.json에 함께 기록한다.
// 사용법: BASE=http://localhost:5174 node shoot.mjs
import { spawn } from 'node:child_process'
import { mkdirSync, writeFileSync, rmSync } from 'node:fs'
import { join } from 'node:path'
import { SPEC } from './spec.mjs'

const BASE = process.env.BASE ?? 'http://localhost:5174'
const OUT = join(import.meta.dirname, 'shots')
const PROFILE = join(import.meta.dirname, '.chrome-shoot')
const PORT = 9333
const W = 460
const H = 920

rmSync(PROFILE, { recursive: true, force: true })
mkdirSync(OUT, { recursive: true })

const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${PROFILE}`,
  '--no-first-run', '--hide-scrollbars', `--window-size=${W},${H}`, 'about:blank',
])

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function getWs() {
  for (let i = 0; i < 50; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
      const page = list.find((t) => t.type === 'page')
      if (page) return page.webSocketDebuggerUrl
    } catch {}
    await sleep(200)
  }
  throw new Error('chrome not ready')
}

const ws = new WebSocket(await getWs())
await new Promise((r) => ws.addEventListener('open', r, { once: true }))
let seq = 0
const pending = new Map()
ws.addEventListener('message', (ev) => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) {
    const { resolve, reject } = pending.get(msg.id)
    pending.delete(msg.id)
    msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result)
  }
})
const send = (method, params = {}) =>
  new Promise((resolve, reject) => {
    const id = ++seq
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params }))
  })

async function js(expr) {
  const r = await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })
  if (r.exceptionDetails) throw new Error(`JS error: ${r.exceptionDetails.exception?.description ?? expr}`)
  return r.result.value
}

await send('Page.enable')
await send('Runtime.enable')
await send('Emulation.setDeviceMetricsOverride', { width: W, height: H, deviceScaleFactor: 2, mobile: false })

const HELPERS = `
(() => {
  if (!document.getElementById('__hide_dt')) {
    const s = document.createElement('style');
    s.id = '__hide_dt';
    s.textContent = '#__vue-devtools-container__, #vue-inspector-container { display: none !important; }';
    document.head.appendChild(s);
  }
})();
window.__h = {
  visible(el){ return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length) },
  byText(text, sel='button,a'){
    return [...document.querySelectorAll(sel)].filter(e=>this.visible(e)).find(e=>e.innerText.replace(/\\s+/g,' ').trim().includes(text))
  },
  click(text, sel){ const el=this.byText(text, sel); if(!el) throw new Error('no element: '+text); el.click(); return true },
  field(label){
    const f=[...document.querySelectorAll('.field')].filter(e=>this.visible(e)).find(e=>(e.querySelector('label,span')?.innerText||'').trim().startsWith(label));
    if(!f) throw new Error('no field: '+label);
    return f.querySelector('input,textarea,select')
  },
  set(el, value){ el.value=value; el.dispatchEvent(new Event(el.tagName==='SELECT'?'change':'input',{bubbles:true})); return true },
  setField(label, value){ return this.set(this.field(label), value) },
  has(text){ return document.body.innerText.includes(text) },
  t(text, sel){ return this.byText(text, sel) },
  q(sel){ return document.querySelector(sel) },
  qa(sel, i){ return [...document.querySelectorAll(sel)][i] },
}
`

async function helpers() { await js(HELPERS) }
async function go(path) {
  await send('Page.navigate', { url: BASE + path })
  await sleep(1200)
  await helpers()
}
async function waitText(text, timeout = 8000) {
  const t0 = Date.now()
  while (Date.now() - t0 < timeout) {
    if (await js(`document.body.innerText.includes(${JSON.stringify(text)})`)) return
    await sleep(150)
  }
  throw new Error('timeout waiting for: ' + text)
}
async function click(text, sel) {
  await js(`__h.click(${JSON.stringify(text)}${sel ? ',' + JSON.stringify(sel) : ''})`)
  await sleep(400)
  await helpers()
}
async function setField(label, value) {
  await js(`__h.setField(${JSON.stringify(label)}, ${JSON.stringify(value)})`)
  await sleep(100)
}

const annots = {}
const missing = []

async function shot(name) {
  await sleep(350)
  const spec = SPEC[name]
  if (spec) {
    const boxes = await js(`(() => {
      const out = {}
      for (const [key, expr, dir] of ${JSON.stringify(spec)}) {
        let el = null
        try { el = eval(expr) } catch (e) {}
        if (!el) { out[key] = null; continue }
        const r = el.getBoundingClientRect()
        out[key] = { x: r.left, y: r.top, w: r.width, h: r.height, dir }
      }
      return out
    })()`)
    for (const [k, v] of Object.entries(boxes)) if (!v) missing.push(`${name}.${k}`)
    annots[name] = Object.fromEntries(Object.entries(boxes).filter(([, v]) => v))
  }
  const { data } = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(join(OUT, `${name}.png`), Buffer.from(data, 'base64'))
  console.log('shot', name, spec ? `(box ${Object.keys(annots[name]).length}/${spec.length})` : '')
}

// Math.random을 고정해 목업 AI의 성공/실패를 결정적으로 만든다.
async function forceRandom(value) { await js(`(()=>{ if(!window.__rnd) window.__rnd=Math.random; Math.random=()=>${value}; })()`) }
async function restoreRandom() { await js(`(()=>{ if(window.__rnd) Math.random=window.__rnd; })()`) }
async function succeedAi() { await js(`(()=>{ if(!window.__rnd) window.__rnd=Math.random; const r=window.__rnd; Math.random=()=>{ let v=r(); return v<0.1?0.1+v:v } })()`) }
async function attachFile(path, name) {
  await js(`(async()=>{
    const blob = await (await fetch(${JSON.stringify(path)})).blob();
    const file = new File([blob], ${JSON.stringify(name)}, { type: 'image/png' });
    const dt = new DataTransfer(); dt.items.add(file);
    const input = document.querySelector('input[type=file]');
    input.files = dt.files;
    input.dispatchEvent(new Event('change', { bubbles: true }));
    return true;
  })()`)
}

try {
  // ---------- 로그인 / 회원가입 ----------
  await go('/login')
  await js('localStorage.clear()')
  await go('/login')
  await shot('01_login')

  await go('/login')
  await setField('이메일', 'demo@today-outfit.app')
  await setField('비밀번호', 'wrongpass')
  await click('로그인', 'button[type=submit]')
  await waitText('올바르지 않습니다')
  await shot('01b_login_error')

  await go('/signup')
  await setField('이름', '김민')
  await setField('이메일', 'kimmin@example.com')
  await setField('비밀번호', 'pass1234')
  await setField('비밀번호 확인', 'pass9999')
  await click('가입하기')
  await waitText('일치하지 않습니다')
  await shot('02_signup_error')

  // ---------- 데모 로그인 → 홈 ----------
  await go('/login')
  await click('데모 계정으로 체험하기')
  await waitText('오늘 뭐 입지?')
  await helpers()
  await sleep(2500) // 데모 시작 토스트가 사라진 뒤 캡처
  await shot('03_home_empty_pick')

  await click('AI 오늘의 픽 받기')
  await js(`document.querySelector('.lock-btn').click()`)
  await sleep(300)
  await shot('04_home_drawn')

  await click('이걸로 입을래요')
  await waitText('확정됨')
  await sleep(2500)
  await shot('05_home_confirmed')

  // ---------- 옷장 ----------
  await go('/wardrobe')
  await shot('06_wardrobe')
  await js(`document.querySelector('.fab').click()`)
  await sleep(500)
  await shot('07_wardrobe_add_sheet')

  await go('/wardrobe')
  await js(`const s=document.querySelectorAll('.filters select')[0]; s.value='BOTTOM'; s.dispatchEvent(new Event('change'))`)
  await sleep(400)
  await helpers()
  await shot('06b_wardrobe_filter')

  // 사진 등록 - 성공
  await go('/wardrobe/add/photo')
  await shot('08a_add_photo_empty')
  await succeedAi()
  await attachFile('/images/black_pants.png', 'black_pants.png')
  await waitText('AI 분석 결과 확인 및 수정')
  await restoreRandom()
  await setField('이름', '블랙 와이드 슬랙스')
  await setField('카테고리', 'BOTTOM')
  await setField('색상', '블랙')
  await helpers()
  await shot('08b_add_photo_result')

  // 사진 등록 - AI 실패
  await go('/wardrobe/add/photo')
  await forceRandom(0.01)
  await attachFile('/images/white_shoes.png', 'white_shoes.png')
  await waitText('수동 등록으로 이동')
  await restoreRandom()
  await helpers()
  await shot('09_add_photo_fail')

  // 문장 등록
  await go('/wardrobe/add/text')
  await js(`__h.set(document.querySelector('textarea'), '베이지 니트, 블루 데님 팬츠, 화이트 스니커즈')`)
  await succeedAi()
  await click('AI로 분석하기')
  await waitText('일괄 저장')
  await restoreRandom()
  await helpers()
  await shot('10_add_text_candidates')

  // 직접 입력
  await go('/wardrobe/add/manual')
  await setField('이름', '그레이 후드티')
  await setField('카테고리', 'TOP')
  await setField('색상', '그레이')
  await setField('계절', 'FALL')
  await helpers()
  await shot('11_add_manual')

  // 의류 상세
  await go('/wardrobe')
  await js(`document.querySelector('.clothing-card').click()`)
  await sleep(700)
  await helpers()
  await shot('12_clothing_detail')

  // ---------- 코디 ----------
  await go('/outfits/ai')
  await js(`__h.set(document.querySelector('textarea'), '친구와 카페에서 만나는 캐주얼한 자리')`)
  await succeedAi()
  await click('AI 코디 추천받기')
  await waitText('추천 결과')
  await restoreRandom()
  await helpers()
  await shot('14_outfit_ai_result')
  await click('이 코디 저장하기')
  await waitText('AI 추천 이유')
  await sleep(2500)
  await helpers()
  await shot('16_outfit_detail')

  await go('/outfits/create')
  await js(`const cards=[...document.querySelectorAll('.clothing-card')]; [0,4,7].forEach(i=>cards[i]?.click())`)
  await sleep(300)
  await setField('코디 이름', '주말 나들이룩')
  await js(`window.scrollTo(0,0)`)
  await helpers()
  await shot('15_outfit_manual')

  await go('/outfits')
  await shot('13_outfits')

  // ---------- 플래너 ----------
  await go('/planner')
  await shot('17_planner')
  await js(`[...document.querySelectorAll('.day-row')].find(r=>r.innerText.includes('+ 코디 배치하기')).click()`)
  await sleep(500)
  await helpers()
  await shot('18_planner_picker')
  await go('/planner')
  await js(`[...document.querySelectorAll('.day-row')].find(r=>!r.innerText.includes('+ 코디 배치하기')).click()`)
  await sleep(500)
  await helpers()
  await shot('19_planner_action')

  await go('/planner')
  await click('AI 주간 코디 추천 열기')
  await setField('상황', '다음 주는 회사 출근이 많아요')
  await setField('일수', '5')
  await succeedAi()
  await click('주간 추천 요청')
  await waitText('플래너에 전체 저장')
  await restoreRandom()
  await js(`window.scrollTo(0, document.body.scrollHeight)`)
  await helpers()
  await shot('20_planner_ai')

  // 코디 상세 → 플래너 배치 모드
  await go('/outfits')
  await js(`document.querySelector('.outfit-card').click()`)
  await sleep(600)
  await helpers()
  await click('플래너에 배치하기')
  await waitText('배치할 날짜를 선택하세요')
  await helpers()
  await shot('19b_planner_place_mode')

  // ---------- 챌린지 ----------
  await go('/challenge')
  await shot('21_challenge_start')
  await click('챌린지 시작하기')
  await sleep(400)
  await helpers()
  await shot('22_challenge_round')
  for (let i = 0; i < 6; i++) {
    const done = await js(`!document.querySelector('.pick-card')`)
    if (done) break
    await js(`document.querySelector('.pick-card').click()`)
    await sleep(350)
  }
  await helpers()
  await succeedAi()
  await waitText('코디로 저장하기')
  await sleep(1500)
  await restoreRandom()
  await helpers()
  await shot('23_challenge_result')

  // ---------- 설정 ----------
  await go('/settings')
  await click('계정 탈퇴')
  await setField('현재 비밀번호', 'wrong')
  await click('최종 탈퇴하기')
  await waitText('올바르지 않습니다')
  await helpers()
  await shot('24_settings_withdraw')

  writeFileSync(join(import.meta.dirname, 'annots.json'), JSON.stringify(annots, null, 2))
  console.log('DONE. annotated shots:', Object.keys(annots).length)
  if (missing.length) console.log('MISSING BOXES:', missing.join(', '))
} catch (e) {
  console.error('FAILED', e.message)
  await shot('zz_error_state').catch(() => {})
  process.exitCode = 1
} finally {
  ws.close()
  chrome.kill()
}

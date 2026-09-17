// HTML → PDF (CDP Page.printToPDF, CSS @page 크기 사용)
// 사용법: IN=deck.html OUT=deck.pdf node print.mjs
import { spawn } from 'node:child_process'
import { writeFileSync, rmSync } from 'node:fs'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'

const DIR = import.meta.dirname
const PORT = 9334
const PROFILE = join(DIR, '.chrome-print')
rmSync(PROFILE, { recursive: true, force: true })

const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${PROFILE}`, '--no-first-run', 'about:blank',
])
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

try {
  let wsUrl
  for (let i = 0; i < 50 && !wsUrl; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
      wsUrl = list.find((t) => t.type === 'page')?.webSocketDebuggerUrl
    } catch {}
    if (!wsUrl) await sleep(200)
  }
  const ws = new WebSocket(wsUrl)
  await new Promise((r) => ws.addEventListener('open', r, { once: true }))
  let seq = 0
  const pending = new Map()
  const events = []
  ws.addEventListener('message', (ev) => {
    const m = JSON.parse(ev.data)
    if (m.id && pending.has(m.id)) {
      const { resolve, reject } = pending.get(m.id)
      pending.delete(m.id)
      m.error ? reject(new Error(JSON.stringify(m.error))) : resolve(m.result)
    } else if (m.method) events.push(m.method)
  })
  const send = (method, params = {}) =>
    new Promise((resolve, reject) => {
      const id = ++seq
      pending.set(id, { resolve, reject })
      ws.send(JSON.stringify({ id, method, params }))
    })

  const inFile = process.env.IN ?? 'deck.html'
  const outFile = process.env.OUT ?? 'deck.pdf'
  await send('Page.enable')
  await send('Page.navigate', { url: pathToFileURL(join(DIR, inFile)).href })
  for (let i = 0; i < 100 && !events.includes('Page.loadEventFired'); i++) await sleep(100)
  await sleep(1000) // 이미지 디코딩 여유
  const { data } = await send('Page.printToPDF', {
    printBackground: true, preferCSSPageSize: true, displayHeaderFooter: false,
    marginTop: 0, marginBottom: 0, marginLeft: 0, marginRight: 0,
  })
  writeFileSync(join(DIR, outFile), Buffer.from(data, 'base64'))
  console.log('printed', outFile)
  ws.close()
} finally {
  chrome.kill()
}

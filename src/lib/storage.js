const PREFIX = 'today-outfit'

export function loadJSON(key, fallback) {
  try {
    const raw = localStorage.getItem(`${PREFIX}:${key}`)
    if (!raw) return fallback
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

export function saveJSON(key, value) {
  try {
    localStorage.setItem(`${PREFIX}:${key}`, JSON.stringify(value))
  } catch {
    // 저장 공간 초과 등은 데모 환경에서 무시한다.
  }
}

export function removeKey(key) {
  localStorage.removeItem(`${PREFIX}:${key}`)
}

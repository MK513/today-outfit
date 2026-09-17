export const CATEGORIES = [
  { value: 'TOP', label: '상의' },
  { value: 'BOTTOM', label: '하의' },
  { value: 'SHOES', label: '신발' },
  { value: 'HAT', label: '모자' },
  { value: 'ACC', label: '액세서리' },
]

export const OUTFIT_SLOT_CATEGORIES = ['TOP', 'BOTTOM', 'SHOES', 'ACC']

export const CHALLENGE_ORDER = ['TOP', 'BOTTOM', 'SHOES', 'HAT', 'ACC']

export const SEASONS = [
  { value: 'SPRING', label: '봄' },
  { value: 'SUMMER', label: '여름' },
  { value: 'FALL', label: '가을' },
  { value: 'WINTER', label: '겨울' },
  { value: 'ALL', label: '사계절' },
]

// 실사 대표 사진이 최소 한 카테고리에서라도 확보된 색상만 선택지로 남긴다.
export const COLORS = ['블랙', '화이트', '그레이', '베이지', '브라운', '블루', '골드']

export function categoryLabel(value) {
  return CATEGORIES.find((c) => c.value === value)?.label ?? value
}

export function seasonLabel(value) {
  return SEASONS.find((s) => s.value === value)?.label ?? value
}

export const OUTFIT_SOURCE_LABEL = {
  AI: 'AI 추천',
  MANUAL: '직접 제작',
  RANDOM: '오늘의 픽',
  CHALLENGE: '챌린지',
}

export function pad2(n) {
  return String(n).padStart(2, '0')
}

export function toDateKey(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

export function todayKey() {
  return toDateKey(new Date())
}

export function addDays(dateKey, days) {
  const d = new Date(`${dateKey}T00:00:00`)
  d.setDate(d.getDate() + days)
  return toDateKey(d)
}

export function startOfWeek(dateKey) {
  const d = new Date(`${dateKey}T00:00:00`)
  const day = (d.getDay() + 6) % 7 // Monday = 0
  d.setDate(d.getDate() - day)
  return toDateKey(d)
}

const WEEKDAY_LABEL = ['월', '화', '수', '목', '금', '토', '일']

export function weekdayLabel(dateKey) {
  const d = new Date(`${dateKey}T00:00:00`)
  const day = (d.getDay() + 6) % 7
  return WEEKDAY_LABEL[day]
}

export function formatDateShort(dateKey) {
  const [, m, d] = dateKey.split('-')
  return `${Number(m)}.${Number(d)}`
}

export function uid(prefix = 'id') {
  return `${prefix}_${Math.random().toString(36).slice(2, 9)}${Date.now().toString(36).slice(-4)}`
}

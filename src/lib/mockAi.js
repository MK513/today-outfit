import { CATEGORIES, COLORS, SEASONS, OUTFIT_SLOT_CATEGORIES } from './constants'
import { categoryPhotoUrl } from './categoryPhotos'

function hasRealPhoto(clothing) {
  return !!(clothing?.fileUrl || categoryPhotoUrl(clothing?.category, clothing?.color))
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)]
}

const NAME_WORDS = {
  TOP: ['니트', '셔츠', '맨투맨', '후드티', '블라우스', '티셔츠'],
  BOTTOM: ['슬랙스', '데님 팬츠', '와이드 팬츠', '치노 팬츠', '스커트'],
  SHOES: ['스니커즈', '로퍼', '부츠', '샌들'],
  HAT: ['볼캡', '버킷햇', '비니'],
  ACC: ['머플러', '벨트', '목걸이', '가방'],
}

/**
 * 사진 분석 목업: 실제로는 업로드 이미지를 분석해야 하지만,
 * 데모 환경에서는 카테고리 키워드를 랜덤 추정해 후보 속성을 반환한다.
 */
export async function analyzePhoto(file) {
  await delay(900)
  if (Math.random() < 0.05) {
    throw new Error('AI_ANALYSIS_FAILED')
  }
  const category = pick(CATEGORIES).value
  const color = pick(COLORS)
  const season = pick(SEASONS).value
  const name = `${color} ${pick(NAME_WORDS[category])}`
  return { name, category, color, season, fileName: file?.name ?? 'photo.jpg' }
}

const CATEGORY_KEYWORDS = {
  TOP: ['셔츠', '니트', '맨투맨', '후드', '블라우스', '티셔츠', '가디건', '자켓', '코트'],
  BOTTOM: ['바지', '슬랙스', '팬츠', '스커트', '치마', '청바지', '데님'],
  SHOES: ['신발', '운동화', '스니커즈', '로퍼', '부츠', '샌들', '구두'],
  HAT: ['모자', '캡', '비니', '버킷햇'],
  ACC: ['목도리', '머플러', '가방', '벨트', '목걸이', '스카프'],
}

const COLOR_KEYWORDS = COLORS

function guessCategory(segment) {
  for (const [cat, words] of Object.entries(CATEGORY_KEYWORDS)) {
    if (words.some((w) => segment.includes(w))) return cat
  }
  return pick(CATEGORIES).value
}

function guessColor(segment) {
  return COLOR_KEYWORDS.find((c) => segment.includes(c)) ?? pick(COLORS)
}

function guessSeason(segment) {
  if (segment.includes('여름') || segment.includes('반팔')) return 'SUMMER'
  if (segment.includes('겨울') || segment.includes('패딩') || segment.includes('코트')) return 'WINTER'
  if (segment.includes('봄')) return 'SPRING'
  if (segment.includes('가을')) return 'FALL'
  return pick(SEASONS).value
}

/**
 * 문장 기반 일괄 등록 목업: 문장을 쉼표/접속어 단위로 분해해 후보를 생성한다.
 */
export async function parseClothesSentence(sentence) {
  await delay(1100)
  if (!sentence?.trim()) {
    throw new Error('AI_PARSE_FAILED')
  }
  if (Math.random() < 0.05) {
    throw new Error('AI_PARSE_FAILED')
  }
  const segments = sentence
    .split(/[,，、]|그리고|이랑|랑|와 |과 /)
    .map((s) => s.trim())
    .filter(Boolean)
  const list = segments.length ? segments : [sentence.trim()]
  return list.slice(0, 8).map((segment) => {
    const category = guessCategory(segment)
    const color = guessColor(segment)
    const season = guessSeason(segment)
    return {
      name: segment.length <= 18 ? segment : `${color} ${pick(NAME_WORDS[category])}`,
      category,
      color,
      season,
    }
  })
}

const REASON_TEMPLATES = [
  (situation) => `"${situation}" 상황에 어울리도록 활동성과 색상 조합을 고려해 골랐습니다.`,
  (situation) => `보유 의류 중 "${situation}"에 맞는 편안하고 무난한 조합을 우선했습니다.`,
  (situation) => `날씨와 "${situation}" 분위기에 맞춰 톤을 맞춘 조합을 제안합니다.`,
]

function buildOutfitFromWardrobe(wardrobe, opts = {}) {
  const { excludeUsedPerCategory = new Map() } = opts
  const result = {}
  for (const cat of OUTFIT_SLOT_CATEGORIES) {
    const pool = wardrobe.filter((c) => c.category === cat && hasRealPhoto(c))
    if (!pool.length) continue
    result[cat] = pick(pool)
  }
  return result
}

/**
 * 상황 기반 AI 코디 추천 목업.
 */
export async function recommendOutfit(situation, wardrobe) {
  await delay(1000)
  if (!situation?.trim()) {
    throw new Error('EMPTY_SITUATION')
  }
  if (!wardrobe.length) {
    throw new Error('NO_CLOTHES')
  }
  const slots = buildOutfitFromWardrobe(wardrobe)
  const clothingIds = Object.values(slots).map((c) => c.id)
  if (!clothingIds.length) {
    throw new Error('NO_CLOTHES')
  }
  const reason = pick(REASON_TEMPLATES)(situation.trim())
  return { clothingIds, aiReason: reason, slots }
}

/**
 * 주간 AI 코디 추천 목업. 의류가 부족하면 만들 수 있는 날짜까지만 반환한다.
 */
export async function recommendWeeklyOutfits(situation, startDate, days, wardrobe) {
  await delay(1300)
  if (!situation?.trim()) {
    throw new Error('EMPTY_SITUATION')
  }
  const hasEnough = OUTFIT_SLOT_ANY(wardrobe)
  if (!hasEnough) {
    throw new Error('NO_CLOTHES')
  }
  const results = []
  for (let i = 0; i < days; i += 1) {
    const slots = buildOutfitFromWardrobe(wardrobe)
    const clothingIds = Object.values(slots).map((c) => c.id)
    if (!clothingIds.length) break
    results.push({
      dayOffset: i,
      clothingIds,
      slots,
      aiReason: pick(REASON_TEMPLATES)(situation.trim()),
    })
  }
  return results
}

function OUTFIT_SLOT_ANY(wardrobe) {
  return OUTFIT_SLOT_CATEGORIES.some((cat) => wardrobe.some((c) => c.category === cat && hasRealPhoto(c)))
}

const TAG_POOL = ['캐주얼', '오피스룩', '스트릿', '미니멀', '컬러포인트', '레이어드', '스포티', '데이트룩']
const COMMENT_POOL = [
  '색상 조합이 안정적이고 데일리로 활용하기 좋아요.',
  '포인트 아이템이 잘 살아나는 개성 있는 조합이에요.',
  '균형 잡힌 실루엣으로 어디에나 잘 어울려요.',
  '톤온톤 매치가 돋보이는 깔끔한 선택이에요.',
]

/**
 * 챌린지 결과 AI 평가 목업.
 */
export async function evaluateChallengeOutfit(clothingIds) {
  await delay(1200)
  if (Math.random() < 0.08) {
    throw new Error('AI_EVAL_FAILED')
  }
  const score = Math.floor(Math.random() * 41) + 60 // 60~100
  const comment = pick(COMMENT_POOL)
  const tags = [...TAG_POOL].sort(() => Math.random() - 0.5).slice(0, 3)
  return { score, comment, tags }
}

export function randomPickByCategory(wardrobe, category, excludeId) {
  const pool = wardrobe.filter((c) => c.category === category && c.id !== excludeId && hasRealPhoto(c))
  if (!pool.length) {
    return wardrobe.find((c) => c.category === category && hasRealPhoto(c)) ?? null
  }
  return pick(pool)
}

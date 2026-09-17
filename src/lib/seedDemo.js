import { uid, todayKey, addDays } from './constants'

const DEMO_EMAIL = 'demo@today-outfit.app'
const DEMO_PASSWORD = 'demo1234'

// 실사 사진이 매칭되는 카테고리+색상 조합만 사용한다.
const DEMO_CLOTHES = [
  { name: '화이트 코튼 셔츠', category: 'TOP', color: '화이트', season: 'ALL' },
  { name: '블랙 맨투맨', category: 'TOP', color: '블랙', season: 'ALL' },
  { name: '그레이 스웨트셔츠', category: 'TOP', color: '그레이', season: 'FALL' },
  { name: '블랙 슬랙스', category: 'BOTTOM', color: '블랙', season: 'ALL' },
  { name: '블루 와이드 데님', category: 'BOTTOM', color: '블루', season: 'ALL' },
  { name: '브라운 치노 팬츠', category: 'BOTTOM', color: '브라운', season: 'SPRING' },
  { name: '화이트 스니커즈', category: 'SHOES', color: '화이트', season: 'ALL' },
  { name: '블랙 첼시부츠', category: 'SHOES', color: '블랙', season: 'WINTER' },
  { name: '블랙 버킷햇', category: 'HAT', color: '블랙', season: 'ALL' },
  { name: '브라운 볼캡', category: 'HAT', color: '브라운', season: 'SUMMER' },
  { name: '그레이 머플러', category: 'ACC', color: '그레이', season: 'WINTER' },
  { name: '블랙 가죽 벨트', category: 'ACC', color: '블랙', season: 'ALL' },
  { name: '베이지 손수건', category: 'ACC', color: '베이지', season: 'ALL' },
  { name: '골드 목걸이', category: 'ACC', color: '골드', season: 'ALL' },
]

export function ensureDemoAccount(authStore, wardrobeStore, outfitsStore, plannerStore) {
  let user = authStore.users.find((u) => u.email === DEMO_EMAIL && !u.withdrawnAt)
  if (user) return user.id

  authStore.users.push({
    id: uid('user'),
    email: DEMO_EMAIL,
    password: DEMO_PASSWORD,
    name: '데모 사용자',
    createdAt: new Date().toISOString(),
    withdrawnAt: null,
  })
  user = authStore.users[authStore.users.length - 1]
  authStore.persist()

  const created = wardrobeStore.addBulk(
    DEMO_CLOTHES.map((c) => ({ ...c, ownerId: user.id, source: 'MANUAL' })),
  )

  const byCat = (cat, index = 0) => created.filter((c) => c.category === cat)[index]
  const outfitA = outfitsStore.add({
    ownerId: user.id,
    name: '오피스 캐주얼',
    memo: '무난한 출근룩',
    clothingIds: [byCat('TOP').id, byCat('BOTTOM').id, byCat('SHOES').id, byCat('ACC').id],
    source: 'MANUAL',
  })
  outfitsStore.add({
    ownerId: user.id,
    name: '주말 나들이룩',
    memo: '편안한 주말 코디',
    clothingIds: [byCat('TOP', 1).id, byCat('BOTTOM', 1).id, byCat('SHOES', 1).id],
    source: 'AI',
    requestText: '가까운 공원 산책',
    aiReason: '"가까운 공원 산책" 상황에 어울리도록 편안한 소재와 색상을 골랐습니다.',
  })

  const today = todayKey()
  plannerStore.upsert({ ownerId: user.id, planDate: addDays(today, 1), outfitId: outfitA.id })

  return user.id
}

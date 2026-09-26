import { todayKey, addDays } from './constants'

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

/**
 * 데모 계정(서버 /auth/demo)으로 로그인한 뒤, 이 기기에 샘플 옷장 데이터가 없으면 채운다.
 * 의류·코디·플래너가 서버로 옮겨지면(3~4단계) 서버가 만든 샘플 데이터를 쓰고 이 함수는 제거한다.
 */
export function seedLocalDemoData(userId, wardrobeStore, outfitsStore, plannerStore) {
  if (wardrobeStore.countByOwner(userId) > 0) return

  const created = wardrobeStore.addBulk(
    DEMO_CLOTHES.map((c) => ({ ...c, ownerId: userId, source: 'MANUAL' })),
  )

  const byCat = (cat, index = 0) => created.filter((c) => c.category === cat)[index]
  const outfitA = outfitsStore.add({
    ownerId: userId,
    name: '오피스 캐주얼',
    memo: '무난한 출근룩',
    clothingIds: [byCat('TOP').id, byCat('BOTTOM').id, byCat('SHOES').id, byCat('ACC').id],
    source: 'MANUAL',
  })
  outfitsStore.add({
    ownerId: userId,
    name: '주말 나들이룩',
    memo: '편안한 주말 코디',
    clothingIds: [byCat('TOP', 1).id, byCat('BOTTOM', 1).id, byCat('SHOES', 1).id],
    source: 'AI',
    requestText: '가까운 공원 산책',
    aiReason: '"가까운 공원 산책" 상황에 어울리도록 편안한 소재와 색상을 골랐습니다.',
  })

  const today = todayKey()
  plannerStore.upsert({ ownerId: userId, planDate: addDays(today, 1), outfitId: outfitA.id })
}

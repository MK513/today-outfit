import { todayKey, addDays } from './constants'

/**
 * 데모 계정(서버 /auth/demo)으로 로그인한 뒤, 이 기기에 샘플 코디 · 플래너가 없으면 채운다.
 * 샘플 의류 14벌은 서버가 만들어 두므로(DemoAccountSeeder), 그 의류로 코디를 구성한다.
 * 코디 · 플래너가 서버로 옮겨지면(4단계) 서버 샘플 데이터를 쓰고 이 파일은 제거한다.
 */
export function seedLocalDemoData(userId, clothes, outfitsStore, plannerStore) {
  if (outfitsStore.byOwner(userId).length > 0) return
  const created = [...clothes].sort((a, b) => a.id - b.id)
  if (!created.length) return

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

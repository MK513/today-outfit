import { defineStore } from 'pinia'
import { api } from '@/lib/api'
import { useOutfitsStore } from '@/stores/outfits'

// 서버(/api/clothes)의 내 옷장을 메모리에 캐시한다.
// 코디 · 플래너 · 챌린지 화면이 의류를 ID로 바로 찾아야 해서 목록 전체를 한 번에 불러온다.
// 항목 모양: 서버 Clothing 스키마(camelCase) + ownerId

const PAGE_SIZE = 100

function withOwner(clothing, ownerId) {
  return { ...clothing, ownerId }
}

export const useWardrobeStore = defineStore('wardrobe', {
  state: () => ({
    clothes: [],
    loadedFor: null, // 불러온 사용자 ID
    loadingPromise: null,
  }),
  getters: {
    byOwner: (state) => (ownerId) => state.clothes.filter((c) => c.ownerId === ownerId),
    // 라우트 파라미터(문자열)와 서버 ID(숫자)를 모두 받는다.
    byId: (state) => (id) => state.clothes.find((c) => String(c.id) === String(id)) ?? null,
    countByOwner: (state) => (ownerId) => state.clothes.filter((c) => c.ownerId === ownerId).length,
    loaded: (state) => state.loadedFor !== null,
  },
  actions: {
    /** 로그인 사용자의 옷장 전체를 불러온다. 이미 불러왔거나 불러오는 중이면 다시 요청하지 않는다. */
    load(ownerId) {
      if (this.loadedFor === ownerId) return Promise.resolve()
      if (this.loadingPromise) return this.loadingPromise
      this.loadingPromise = (async () => {
        try {
          const all = []
          for (let page = 1; ; page += 1) {
            const res = await api.get(`/clothes?page=${page}&size=${PAGE_SIZE}`)
            all.push(...res.content)
            if (page >= res.totalPages) break
          }
          this.clothes = all.map((c) => withOwner(c, ownerId))
          this.loadedFor = ownerId
        } finally {
          this.loadingPromise = null
        }
      })()
      return this.loadingPromise
    },
    reset() {
      this.clothes = []
      this.loadedFor = null
    },
    filtered(ownerId, { category, season, color } = {}) {
      return this.clothes.filter((c) => {
        if (c.ownerId !== ownerId) return false
        if (category && c.category !== category) return false
        if (season && c.season !== season) return false
        if (color && c.color !== color) return false
        return true
      })
    },
    /** 사진을 먼저 올리고 받은 { imageUrl, imageFileName }을 add()에 넘긴다. */
    uploadImage(file) {
      return api.upload('/images', { image: file })
    },
    async add({ ownerId, name, category, color, season, imageUrl, imageFileName, source }) {
      const created = await api.post('/clothes', {
        name,
        category,
        color,
        season,
        source,
        imageUrl: imageUrl ?? null,
        imageFileName: imageFileName ?? null,
      })
      const item = withOwner(created, ownerId)
      this.clothes.unshift(item)
      return item
    },
    /** 한 건이라도 실패하면 서버가 전체를 저장하지 않는다. */
    async addBulk(ownerId, items) {
      const created = await api.post('/clothes/bulk', { items })
      const list = created.map((c) => withOwner(c, ownerId))
      this.clothes.unshift(...list)
      return list
    },
    async remove(id) {
      await api.delete(`/clothes/${id}`)
      this.clothes = this.clothes.filter((c) => String(c.id) !== String(id))
      // 서버에서는 코디 구성에서 자동으로 빠진다. 코디가 아직 로컬에 있어 같은 처리를 해준다(4단계에서 제거).
      useOutfitsStore().removeClothingReference(id)
    },
  },
})

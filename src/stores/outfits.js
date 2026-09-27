import { defineStore } from 'pinia'
import { api } from '@/lib/api'

// 코디는 캐시하지 않고 화면마다 서버(/api/outfits)에서 받아온다.
// - 목록 항목(OutfitSummary): { id, name, source, aiScore, thumbnails: ClothingSummary[], createdAt }
// - 상세(OutfitDetail): 위 필드 + { memo, requestText, aiReason, aiComment, aiTags, items: [{ itemOrder, clothing }] }

const LIST_SIZE = 100

export const useOutfitsStore = defineStore('outfits', {
  actions: {
    /** 내 코디 최신순 (최대 100개). source로 생성 경로를 거를 수 있다. */
    async list({ source } = {}) {
      const query = source ? `&source=${source}` : ''
      const page = await api.get(`/outfits?size=${LIST_SIZE}${query}`)
      return page.content
    },
    get(id) {
      return api.get(`/outfits/${id}`)
    },
    /**
     * @param payload { name, memo, source, clothingIds, requestText, aiReason, aiScore, aiComment, aiTags }
     *   생성 경로와 맞지 않는 AI 필드는 서버가 무시한다.
     * @returns 저장된 코디 상세
     */
    create(payload) {
      return api.post('/outfits', payload)
    },
    /** 배치된 플래너 일정도 서버에서 함께 삭제된다. */
    remove(id) {
      return api.delete(`/outfits/${id}`)
    },
  },
})

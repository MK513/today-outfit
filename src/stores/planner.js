import { defineStore } from 'pinia'
import { api } from '@/lib/api'

// 현재 화면이 보고 있는 기간(주간 플래너의 7일, 홈의 오늘)의 일정만 날짜별로 보관한다.
// 일정 모양: { id, planDate, outfit: OutfitSummary, updatedAt }

export const usePlannerStore = defineStore('planner', {
  state: () => ({
    byDate: {},
  }),
  getters: {
    findByDate: (state) => (planDate) => state.byDate[planDate] ?? null,
  },
  actions: {
    /** start~end(포함, 최대 31일) 일정을 불러와 보관 내용을 교체한다. */
    async loadRange(start, end) {
      const list = await api.get(`/planner/schedules?start_date=${start}&end_date=${end}`)
      this.byDate = Object.fromEntries(list.map((s) => [s.planDate, s]))
    },
    /** 날짜에 코디를 배치한다. 새로 배치했으면 created = true, 기존 배치를 교체했으면 false. */
    async upsert(planDate, outfitId) {
      const { status, data } = await api.put(`/planner/schedules/${planDate}`, { outfitId }, { withStatus: true })
      this.byDate = { ...this.byDate, [planDate]: data }
      return { created: status === 201, schedule: data }
    },
    async remove(planDate) {
      await api.delete(`/planner/schedules/${planDate}`)
      const { [planDate]: _removed, ...rest } = this.byDate
      this.byDate = rest
    },
    /**
     * AI 주간 추천 결과를 한 번에 저장한다(서버 한 트랜잭션).
     * @param payload { requestText, days: [{ planDate, clothingIds, aiReason }] }
     */
    async saveWeekly(payload) {
      const saved = await api.post('/planner/weekly-outfits', payload)
      this.byDate = { ...this.byDate, ...Object.fromEntries(saved.map((s) => [s.planDate, s])) }
      return saved
    },
    reset() {
      this.byDate = {}
    },
  },
})

import { defineStore } from 'pinia'
import { api } from '@/lib/api'

// 현재 화면이 보고 있는 기간(주간 플래너의 7일, 홈의 오늘)의 일정만 날짜별로 보관한다.
// 일정 모양: { id, planDate, outfit: OutfitSummary, updatedAt }

export const usePlannerStore = defineStore('planner', {
  state: () => ({
    byDate: {},
    // 가장 최근에 요청한 기간. 주를 빠르게 넘길 때 늦게 도착한 이전 기간 응답을 버리는 데 쓴다.
    requestedRange: null,
  }),
  getters: {
    findByDate: (state) => (planDate) => state.byDate[planDate] ?? null,
  },
  actions: {
    /**
     * start~end(포함, 최대 31일) 일정을 불러와 보관 내용을 교체한다.
     * 응답이 오기 전에 다른 기간을 요청했다면 이 응답은 버린다(화면이 보고 있는 기간과 어긋나지 않게).
     */
    async loadRange(start, end) {
      const range = `${start}~${end}`
      this.requestedRange = range
      const list = await api.get(`/planner/schedules?start_date=${start}&end_date=${end}`)
      if (this.requestedRange !== range) return
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

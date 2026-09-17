import { defineStore } from 'pinia'
import { loadJSON, saveJSON } from '@/lib/storage'
import { uid } from '@/lib/constants'

export const usePlannerStore = defineStore('planner', {
  state: () => ({
    schedules: loadJSON('schedules', []),
  }),
  getters: {
    byOwner: (state) => (ownerId) => state.schedules.filter((s) => s.ownerId === ownerId),
    findByDate: (state) => (ownerId, planDate) =>
      state.schedules.find((s) => s.ownerId === ownerId && s.planDate === planDate) ?? null,
  },
  actions: {
    persist() {
      saveJSON('schedules', this.schedules)
    },
    upsert({ ownerId, planDate, outfitId }) {
      const existing = this.schedules.find((s) => s.ownerId === ownerId && s.planDate === planDate)
      if (existing) {
        existing.outfitId = outfitId
        this.persist()
        return { status: 200, schedule: existing }
      }
      const schedule = {
        id: uid('sched'),
        ownerId,
        planDate,
        outfitId,
        createdAt: new Date().toISOString(),
      }
      this.schedules.unshift(schedule)
      this.persist()
      return { status: 201, schedule }
    },
    remove(scheduleId) {
      this.schedules = this.schedules.filter((s) => s.id !== scheduleId)
      this.persist()
    },
    removeByOutfitId(outfitId) {
      const before = this.schedules.length
      this.schedules = this.schedules.filter((s) => s.outfitId !== outfitId)
      if (this.schedules.length !== before) this.persist()
    },
    purgeOwner(ownerId) {
      this.schedules = this.schedules.filter((s) => s.ownerId !== ownerId)
      this.persist()
    },
  },
})

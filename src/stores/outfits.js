import { defineStore } from 'pinia'
import { loadJSON, saveJSON } from '@/lib/storage'
import { uid } from '@/lib/constants'
import { usePlannerStore } from '@/stores/planner'

export const useOutfitsStore = defineStore('outfits', {
  state: () => ({
    outfits: loadJSON('outfits', []),
  }),
  getters: {
    byOwner: (state) => (ownerId) =>
      state.outfits
        .filter((o) => o.ownerId === ownerId)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)),
    byId: (state) => (id) => state.outfits.find((o) => o.id === id) ?? null,
  },
  actions: {
    persist() {
      saveJSON('outfits', this.outfits)
    },
    add({
      ownerId,
      name,
      memo = '',
      clothingIds,
      source,
      requestText = null,
      aiReason = null,
      aiScore = null,
      aiComment = null,
      aiTags = null,
    }) {
      const outfit = {
        id: uid('outfit'),
        ownerId,
        name,
        memo,
        clothingIds,
        source,
        requestText,
        aiReason,
        aiScore,
        aiComment,
        aiTags,
        createdAt: new Date().toISOString(),
      }
      this.outfits.unshift(outfit)
      this.persist()
      return outfit
    },
    remove(id) {
      this.outfits = this.outfits.filter((o) => o.id !== id)
      this.persist()
      usePlannerStore().removeByOutfitId(id)
    },
    removeClothingReference(clothingId) {
      let changed = false
      this.outfits = this.outfits.map((o) => {
        if (!o.clothingIds.includes(clothingId)) return o
        changed = true
        return { ...o, clothingIds: o.clothingIds.filter((id) => id !== clothingId) }
      })
      if (changed) this.persist()
    },
    purgeOwner(ownerId) {
      this.outfits = this.outfits.filter((o) => o.ownerId !== ownerId)
      this.persist()
    },
  },
})

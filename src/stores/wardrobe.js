import { defineStore } from 'pinia'
import { loadJSON, saveJSON } from '@/lib/storage'
import { uid } from '@/lib/constants'
import { useOutfitsStore } from '@/stores/outfits'

export const useWardrobeStore = defineStore('wardrobe', {
  state: () => ({
    clothes: loadJSON('clothes', []),
  }),
  getters: {
    byOwner: (state) => (ownerId) => state.clothes.filter((c) => c.ownerId === ownerId),
    byId: (state) => (id) => state.clothes.find((c) => c.id === id) ?? null,
    countByOwner: (state) => (ownerId) => state.clothes.filter((c) => c.ownerId === ownerId).length,
  },
  actions: {
    persist() {
      saveJSON('clothes', this.clothes)
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
    add({ ownerId, name, category, color, season, fileUrl, fileName, source }) {
      const item = {
        id: uid('cloth'),
        ownerId,
        name,
        category,
        color,
        season,
        fileUrl: fileUrl ?? null,
        fileName: fileName ?? null,
        source,
        createdAt: new Date().toISOString(),
      }
      this.clothes.unshift(item)
      this.persist()
      return item
    },
    addBulk(items) {
      const created = items.map((item) => ({
        id: uid('cloth'),
        createdAt: new Date().toISOString(),
        fileUrl: null,
        fileName: null,
        ...item,
      }))
      this.clothes.unshift(...created)
      this.persist()
      return created
    },
    remove(id) {
      this.clothes = this.clothes.filter((c) => c.id !== id)
      this.persist()
      useOutfitsStore().removeClothingReference(id)
    },
    purgeOwner(ownerId) {
      this.clothes = this.clothes.filter((c) => c.ownerId !== ownerId)
      this.persist()
    },
  },
})

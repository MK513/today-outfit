import { defineStore } from 'pinia'

export const useChallengeStore = defineStore('challenge', {
  state: () => ({
    rounds: [], // [{ category, left, right }]
    choices: [], // clothingId per completed round, in round order
    blurMode: false,
  }),
  getters: {
    chosenClothingIds: (state) => state.choices,
  },
  actions: {
    start(rounds) {
      this.rounds = rounds
      this.choices = []
    },
    choose(clothingId) {
      this.choices.push(clothingId)
    },
    reset() {
      this.rounds = []
      this.choices = []
    },
  },
})

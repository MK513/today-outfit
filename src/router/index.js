import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChallengeStore } from '@/stores/challenge'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
  { path: '/signup', name: 'signup', component: () => import('@/views/SignupView.vue'), meta: { guest: true } },
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
  { path: '/wardrobe', name: 'wardrobe', component: () => import('@/views/WardrobeListView.vue') },
  {
    path: '/wardrobe/add/photo',
    name: 'wardrobe-add-photo',
    component: () => import('@/views/ClothingAddPhotoView.vue'),
  },
  {
    path: '/wardrobe/add/text',
    name: 'wardrobe-add-text',
    component: () => import('@/views/ClothingAddTextView.vue'),
  },
  {
    path: '/wardrobe/add/manual',
    name: 'wardrobe-add-manual',
    component: () => import('@/views/ClothingAddManualView.vue'),
  },
  {
    path: '/wardrobe/:id',
    name: 'wardrobe-detail',
    component: () => import('@/views/ClothingDetailView.vue'),
    props: true,
  },
  { path: '/outfits', name: 'outfits', component: () => import('@/views/OutfitListView.vue') },
  { path: '/outfits/ai', name: 'outfits-ai', component: () => import('@/views/OutfitAiCreateView.vue') },
  {
    path: '/outfits/create',
    name: 'outfits-manual-create',
    component: () => import('@/views/OutfitManualCreateView.vue'),
  },
  {
    path: '/outfits/:id',
    name: 'outfit-detail',
    component: () => import('@/views/OutfitDetailView.vue'),
    props: true,
  },
  { path: '/planner', name: 'planner', component: () => import('@/views/PlannerView.vue') },
  { path: '/challenge', name: 'challenge', component: () => import('@/views/ChallengeView.vue') },
  {
    path: '/challenge/result',
    name: 'challenge-result',
    component: () => import('@/views/ChallengeResultView.vue'),
    beforeEnter: () => {
      const challenge = useChallengeStore()
      if (!challenge.chosenClothingIds.length) {
        return { name: 'challenge' }
      }
      return true
    },
  },
  { path: '/settings', name: 'settings', component: () => import('@/views/SettingsView.vue') },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.guest && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && auth.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router

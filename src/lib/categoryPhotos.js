import topBlack from '@/assets/category-photos/top_black.webp'
import topWhite from '@/assets/category-photos/top_white.webp'
import topGray from '@/assets/category-photos/top_gray.webp'
import bottomBlack from '@/assets/category-photos/bottom_black.webp'
import bottomBlue from '@/assets/category-photos/bottom_blue.webp'
import bottomBrown from '@/assets/category-photos/bottom_brown.webp'
import shoesBlack from '@/assets/category-photos/shoes_black.webp'
import shoesWhite from '@/assets/category-photos/shoes_white.webp'
import hatBlack from '@/assets/category-photos/hat_black.webp'
import hatBrown from '@/assets/category-photos/hat_brown.webp'
import accBeige from '@/assets/category-photos/acc_beige.webp'
import accGray from '@/assets/category-photos/acc_gray.webp'
import accBlack from '@/assets/category-photos/acc_black.webp'
import accGold from '@/assets/category-photos/acc_gold.webp'

// 카테고리 + 색상 조합에 맞는 실사 대표 사진이 있는 경우에만 매핑한다.
// 매핑이 없는 조합은 ClothingThumb에서 아이콘 폴백으로 처리된다.
const CATEGORY_PHOTOS = {
  TOP: { 블랙: topBlack, 화이트: topWhite, 그레이: topGray },
  BOTTOM: { 블랙: bottomBlack, 블루: bottomBlue, 브라운: bottomBrown },
  SHOES: { 블랙: shoesBlack, 화이트: shoesWhite },
  HAT: { 블랙: hatBlack, 브라운: hatBrown },
  ACC: { 베이지: accBeige, 그레이: accGray, 블랙: accBlack, 골드: accGold },
}

export function categoryPhotoUrl(category, color) {
  return CATEGORY_PHOTOS[category]?.[color] ?? null
}

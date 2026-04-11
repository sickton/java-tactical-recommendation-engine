export const MOMENT_IMAGES = [
  '/images/moments/match-1.jpg',
  '/images/moments/match-2.jpg',
  '/images/moments/match-3.jpg',
  '/images/moments/match-4.jpg',
  '/images/moments/match-5.jpg',
  '/images/moments/match-6.jpg',
];

export function getMomentImage(index: number): string {
  return MOMENT_IMAGES[index % MOMENT_IMAGES.length];
}

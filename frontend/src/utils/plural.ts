// Русское склонение по числу: forms = [для 1, для 2–4, для 5+]
// Примеры: pluralRu(1, ['отзыв','отзыва','отзывов']) → 'отзыв', pluralRu(2, …) → 'отзыва', pluralRu(5, …) → 'отзывов'
export function pluralRu(count: number, forms: [string, string, string]): string {
    const n = Math.abs(count) % 100;
    const n1 = n % 10;
    if (n > 10 && n < 20) return forms[2];
    if (n1 > 1 && n1 < 5) return forms[1];
    if (n1 === 1) return forms[0];
    return forms[2];
}

export const reviewsWord = (count: number): string =>
    pluralRu(count, ['отзыв', 'отзыва', 'отзывов']);

export const purchasesWord = (count: number): string =>
    pluralRu(count, ['покупка', 'покупки', 'покупок']);

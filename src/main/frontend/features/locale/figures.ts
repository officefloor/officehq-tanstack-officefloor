import type { Locale } from './localeQuery';

/** A figure: the whole text is a number with two decimals, as the screen shows amounts, dues, totals, rates, shares and cuts. */
const FIGURE = /^-?\d+\.\d{2}$/;

/** The figure as shown under the locale; any other text is left as it is. */
export function showFigure(text: string, locale: Locale): string {
  if (locale === 'en' || !FIGURE.test(text)) return text;
  const [whole, decimals] = text.split('.');
  const sign = whole.startsWith('-') ? '-' : '';
  const digits = sign ? whole.slice(1) : whole;
  return `${sign}${digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${decimals}`;
}

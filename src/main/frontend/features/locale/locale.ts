import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// The locale is per user, not per sheet: it decides how the screen shows that user figures.
export const LOCALES = ['en', 'de'] as const;
export type Locale = (typeof LOCALES)[number];

export const localeKey = (user: string) => ['user', user, 'locale'] as const;

/** The signed-in user's locale: en until they choose another. */
export function useLocale(user: string) {
  return useQuery({
    queryKey: localeKey(user),
    queryFn: async () =>
      (await getJson<{ locale: Locale }>('/api/locale', user)).locale,
  });
}

// A figure as the screen shows it under en: digits, a dot, exactly two decimals.
const FIGURE = /^(\s*)(-?)(\d+)\.(\d{2})(\s*)$/;

/** A figure's text as shown under `locale`; any other text unchanged. Under de, 1234.56 is
 *  1.234,56: a dot every three digits and a comma before the decimals. */
export function showFigure(text: string, locale: Locale): string {
  if (locale !== 'de') return text;
  const m = FIGURE.exec(text);
  if (!m) return text;
  const whole = m[3].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${m[1]}${m[2]}${whole},${m[4]}${m[5]}`;
}

import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The locales a user may choose: how the screen shows that user figures. Entries are typed with a dot either way. */
export const LOCALES = ['en', 'de'] as const;
export type Locale = (typeof LOCALES)[number];

export type UserLocale = { user: string; locale: Locale };

/** Kept per user, not per sheet, so loading a sheet does not drop it. */
export const localeKey = (user: string) => ['user', user, 'locale'] as const;

export function useLocale(user: string) {
  return useQuery({
    queryKey: localeKey(user),
    queryFn: () => getJson<UserLocale>(`/api/users/${encodeURIComponent(user)}/locale`),
    enabled: user !== '',
  });
}

// A figure as the app writes it: a number with two decimals, a dot before them and no grouping.
const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** The figure as the locale shows it: under de 1.234,56 (a dot every three digits, a comma before the decimals);
 *  under en, or for text that is not a figure, unchanged. */
export function showFigure(text: string, locale: Locale): string {
  const m = locale === 'de' ? FIGURE.exec(text.trim()) : null;
  if (!m) return text;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${m[3]}`;
}

import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';

/** How figures are shown to a user: en (1234.56) or de (1.234,56). Entries are always typed with a dot. */
export type Locale = 'en' | 'de';

export const LOCALES: Locale[] = ['en', 'de'];

export type UserLocale = { user: string; locale: Locale };

// Kept per user, not per sheet: not under ['sheet', ...].
export const localeKey = (user: string) => ['user', user, 'locale'] as const;

export function localePath(user: string): string {
  return `/api/users/${encodeURIComponent(user)}/locale`;
}

/** The signed-in user's locale ('en' until it is read, or until they choose one). */
export function useLocale(user: string): Locale {
  const q = useQuery({
    queryKey: localeKey(user),
    queryFn: () => getJsonNow<UserLocale>(localePath(user)),
    enabled: user !== '',
  });
  return q.data?.locale ?? 'en';
}

/** A figure: a number with two decimals, the whole of its text. */
const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** The figure as shown under the locale; any other text unchanged. */
export function formatFigure(text: string, locale: Locale): string {
  if (locale !== 'de') return text;
  const m = FIGURE.exec(text.trim());
  if (!m) return text;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${m[3]}`;
}

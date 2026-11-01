import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The locales a user may choose; en is that of a user who has not chosen. */
export const LOCALES = ['en', 'de'] as const;

export const DEFAULT_LOCALE = 'en';

export type UserLocale = { user: string; locale: string };

export const localeKey = (user: string) => ['user', user, 'locale'] as const;

export const localePath = (user: string) => `/api/users/${encodeURIComponent(user)}/locale`;

/** The signed-in user's locale, kept per user (the same on every sheet). */
export function useLocale(user: string): string {
  const { data } = useQuery({
    queryKey: localeKey(user),
    queryFn: () => getJson<UserLocale>(localePath(user)),
    staleTime: 0,
  });
  return data?.locale ?? DEFAULT_LOCALE;
}

/** A figure as the screen shows it: a number with two decimals, written with a dot. */
export const FIGURE = /^-?\d+\.\d{2}$/;

/** A figure ("1234.56") as shown under a locale: under de with a dot every three digits and a
 *  comma before the decimals ("1.234,56"); under en as it is. */
export function showFigure(figure: string, locale: string): string {
  if (locale !== 'de' || !FIGURE.test(figure)) return figure;
  const negative = figure.startsWith('-');
  const [whole, decimals] = (negative ? figure.slice(1) : figure).split('.');
  return `${negative ? '-' : ''}${whole.replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${decimals}`;
}

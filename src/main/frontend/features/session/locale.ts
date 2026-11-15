import { useQuery } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { useUser } from './session';

/** How the screen shows a user figures: en `1234.56`, de `1.234,56`. Saved per user. */
export const LOCALES = ['en', 'de'] as const;
export type Locale = (typeof LOCALES)[number];
export type UserLocale = { user: string; locale: Locale };

export const localeKey = (user: string) => ['user', user, 'locale'] as const;

export function getLocale(user: string): Promise<UserLocale> {
  return getJson<UserLocale>(`/api/users/${encodeURIComponent(user)}/locale`);
}

/** Save the user's locale; refused (rejects) for a locale the app does not know. */
export function saveLocale(user: string, locale: string): Promise<UserLocale> {
  return putJson<UserLocale>(`/api/users/${encodeURIComponent(user)}/locale`, { locale });
}

/** The signed-in user's locale; en until read, and when no one is signed in. */
export function useLocale(): Locale {
  const [user] = useUser();
  const q = useQuery({ queryKey: localeKey(user ?? ''), queryFn: () => getLocale(user!), enabled: !!user });
  return q.data?.locale ?? 'en';
}

const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** A figure as the locale shows it: text that is wholly a number with two decimals is, under de,
 *  `1.234,56` (a dot every three digits, a comma before the decimals); anything else, and every
 *  text under en, is shown as it is. */
export function formatFigure(text: string, locale: Locale): string {
  if (locale !== 'de') return text;
  const m = FIGURE.exec(text);
  if (!m) return text;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${m[3]}`;
}

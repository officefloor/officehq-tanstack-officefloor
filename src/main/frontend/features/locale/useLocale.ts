import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Locale = 'en' | 'de';
export const LOCALES: Locale[] = ['en', 'de'];

/** Kept per user, not per sheet: the key sits outside ['sheet', …]. */
export const localeKey = (user: string) => ['user', user, 'locale'] as const;

/** The signed-in user's locale: how figures are shown to them. 'en' until they choose (and while loading). */
export function useLocale(user: string): Locale {
  const { data } = useQuery({
    queryKey: localeKey(user),
    queryFn: () => getJson<{ locale: Locale }>(`/api/users/${encodeURIComponent(user)}/locale`),
    enabled: user !== '',
  });
  return data?.locale ?? 'en';
}

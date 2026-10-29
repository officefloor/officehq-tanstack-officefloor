import { getJson } from '../../api/http';

/** How figures are shown to a user: `en` as stored (1234.56), `de` with a dot every three digits and a comma before the decimals (1.234,56). */
export type Locale = 'en' | 'de';

export const LOCALES: Locale[] = ['en', 'de'];

/** Kept per user, not per sheet: the key is outside every sheet's key. */
export const localeKey = (user: string) => ['user', user, 'locale'] as const;

export const localeQuery = (user: string) => ({
  queryKey: localeKey(user),
  queryFn: () => getJson<{ locale: Locale }>(`/api/users/${encodeURIComponent(user)}/locale`).then((r) => r.locale),
});

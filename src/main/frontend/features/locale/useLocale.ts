import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';
import { useSignedInUser } from '../session/useSession';

export const LOCALES = ['en', 'de'] as const;
export type UserLocale = { locale: string };

/** The signed-in user's locale is theirs, not a sheet's: kept under its own key, not dropped when a sheet loads. */
export const localeKey = (user: string) => ['user', user, 'locale'];

/** The signed-in user's locale ('en' until they choose, and while it is being read). */
export function useLocale(): string {
  const [user] = useSignedInUser();
  const locale = useQuery({
    queryKey: localeKey(user),
    queryFn: () => getJsonNow<UserLocale>('/api/locale'),
    enabled: user !== '',
  });
  return locale.data?.locale ?? 'en';
}

/** A figure: the whole text a number with two decimals. */
const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** A shown value as this locale shows it: under 'de' a figure reads 1.234,56 (a dot every three digits, a comma
 *  before the decimals); anything else, and every value under 'en', is shown as it is. Only what the screen shows
 *  goes through this: entries are typed with a dot, and exports and audit lines keep the dot. */
export function figureIn(locale: string, value: string): string {
  const m = locale === 'de' ? FIGURE.exec(value.trim()) : null;
  if (!m) return value;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+$)/g, '.')},${m[3]}`;
}

/** Shows a value as the signed-in user's locale does (figureIn); a value that is not text is shown as it is. */
export function useFigure(): <T>(value: T) => T | string {
  const locale = useLocale();
  return <T,>(value: T) => (typeof value === 'string' ? figureIn(locale, value) : value);
}

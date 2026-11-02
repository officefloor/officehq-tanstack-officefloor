import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';
import { byUser } from '../session/session';

/** How the screen shows figures to a user: en as they are kept (1234.56), de with a dot every three digits and a
 *  comma before the decimals (1.234,56). Entries are typed, and exports and audit lines written, as en whatever it is. */
export type Locale = 'en' | 'de';

export const LOCALES: Locale[] = ['en', 'de'];

export type UserLocale = { locale: Locale };

export const localeKey = (sheet: string, user: string) => ['locale', sheet, user] as const;

export const localePath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/locale`;

/** The signed-in user's locale on the sheet (en unless chosen); read as it is now, whatever the as-of date. */
export function useLocale(sheet: string, user: string) {
  return useQuery({
    queryKey: localeKey(sheet, user),
    queryFn: () => getJsonNow<UserLocale>(byUser(localePath(sheet), user)),
    enabled: sheet !== '' && user !== '',
  });
}

const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** A text as the locale shows it: a whole text that is a number with two decimals is a figure, shown in the
 *  locale's form; any other text is shown as it is. */
export function showFigure(text: string, locale: Locale | undefined): string {
  if (locale !== 'de') return text;
  const m = FIGURE.exec(text);
  if (m === null) return text;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${m[3]}`;
}

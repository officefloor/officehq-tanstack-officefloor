import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey } from '../sheet/sheet';

export type Locale = 'en' | 'de';
export const LOCALES: Locale[] = ['en', 'de'];

/** Under the sheet's key, so loading the sheet again re-reads the user's locale. */
export const localeKey = (sheet: string, user: string) => [...sheetKey(sheet), 'locale', user] as const;

/** The locale the signed-in user has chosen on the sheet ('en' until they choose). Not as of a date: it
 *  is how the screen shows figures, not part of the sheet. */
export function useLocale(sheet: string, user: string) {
  return useQuery({
    queryKey: localeKey(sheet, user),
    queryFn: () =>
      getJson<{ locale: Locale }>(`/api/sheets/${encodeURIComponent(sheet)}/locale?user=${encodeURIComponent(user)}`),
    enabled: sheet !== '' && user !== '',
  });
}

const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** A figure as the locale shows it: under 'de' a text that is wholly a number with two decimals is shown
 *  as 1.234,56; anything else, and every figure under 'en', is shown as it is. */
export function formatFigure(value: string, locale: Locale): string {
  const m = locale === 'de' ? FIGURE.exec(value) : null;
  if (!m) return value;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+$)/g, '.')},${m[3]}`;
}

/** Formats a figure for the signed-in user's screen. Use it on every figure shown (never on an entry
 *  typed, an export or an audit line): `const fig = useFigure(); <td>{fig(total)}</td>`. */
export function useFigure(): (value: string | number | null | undefined) => string {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const locale = useLocale(sheet, user).data?.locale ?? 'en';
  return (value) => (value === null || value === undefined ? '' : formatFigure(String(value), locale));
}

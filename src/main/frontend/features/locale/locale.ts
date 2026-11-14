import { useQuery } from '@tanstack/react-query';
import { getLiveJson } from '../../api/http';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

/** A locale the screen can show figures in. */
export type Locale = 'en' | 'de';
export const LOCALES: Locale[] = ['en', 'de'];

/** The query key for the signed-in user's locale; under sheetKey, so re-loading the sheet re-reads it. */
export const localeKey = (sheet: string, user: string) => [...sheetKey(sheet), 'locale', user] as const;

/** The signed-in user's locale: en unless they chose another. */
export function useLocale() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: localeKey(sheet, user),
    queryFn: () =>
      getLiveJson<{ user: string; locale: Locale }>(
        `/api/sheets/${encodeURIComponent(sheet)}/locale?user=${encodeURIComponent(user)}`,
      ),
    enabled: !!sheet && !!user,
  });
}

// A figure is a cell whose whole text is a number with two decimals, as the app renders it (en).
const FIGURE = /^-?\d+\.\d{2}$/;

/** The figure `text` (1234.56) as the locale shows it: under de 1.234,56, under en unchanged. */
export function showFigure(text: string, locale: Locale): string {
  if (locale !== 'de' || !FIGURE.test(text)) return text;
  const negative = text.startsWith('-');
  const [whole, decimals] = (negative ? text.slice(1) : text).split('.');
  return `${negative ? '-' : ''}${whole.replace(/\B(?=(\d{3})+$)/g, '.')},${decimals}`;
}

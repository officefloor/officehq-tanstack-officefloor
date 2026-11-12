import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** A person's opening balance for a period: the period's name, the person's id and the amount (two decimals),
 *  the sum of the amounts pasted for them in it. */
export type Opening = { period: string; personId: number; amount: string };

/** Under the sheet's key, so loading the sheet again re-reads its opening balances. */
export const openingsKey = (sheet: string) => [...sheetKey(sheet), 'openings'] as const;

/** Every opening balance on the sheet. */
export function useOpenings(sheet: string) {
  return useQuery({
    queryKey: openingsKey(sheet),
    queryFn: () => getJson<Opening[]>(`/api/sheets/${encodeURIComponent(sheet)}/openings`),
    enabled: sheet !== '',
  });
}

/** The person's opening balance for the period, 0.00 when none was pasted. */
export function openingOf(openings: Opening[] | undefined, period: string, personId: number): string {
  return (openings ?? []).find((o) => o.period === period && o.personId === personId)?.amount ?? '0.00';
}

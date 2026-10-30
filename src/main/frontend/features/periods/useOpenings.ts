import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's opening balance for a period: the amounts pasted for them and it, added up (two decimals). */
export type Opening = { personId: string; period: string; amount: string };

export const openingsKey = (sheet: string) => ['sheet', sheet, 'openings'] as const;

/** The sheet's opening balances, each person's for each period that has one. */
export function useOpenings(sheet: string): Opening[] {
  const { data } = useQuery({
    queryKey: openingsKey(sheet),
    queryFn: () => getJson<Opening[]>(`/api/sheets/${encodeURIComponent(sheet)}/openings`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

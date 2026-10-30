import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate: a percent shown with two decimals ("12.50"). */
export type Rate = { grade: string; rate: string };

export const ratesKey = (sheet: string) => ['sheet', sheet, 'rates'] as const;

/** The sheet's rates, by grade. A grade with no rate is absent. */
export function useRates(sheet: string): Map<string, string> {
  const { data } = useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`),
    enabled: sheet !== '',
  });
  return new Map((data ?? []).map((r) => [r.grade, r.rate]));
}

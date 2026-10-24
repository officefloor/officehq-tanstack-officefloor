import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A person's plan as the store holds it: an amount (two decimals), a start date, every N days (at least 1) and an end
 *  date (not before the start), and how many claims it has made so far. */
export type Plan = { id: number; personId: number; amount: string; start: string; every: number; end: string; generated: number };

export const plansQueryKey = (sheetId: string) => ['sheet', sheetId, 'plans'] as const;

export const plansPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/plans`;

/** The sheet's plans, in the order they were added. */
export function usePlans() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: plansQueryKey(sheet ?? ''),
    queryFn: () => getJson<Plan[]>(plansPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

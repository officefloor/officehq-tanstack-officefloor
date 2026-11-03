import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: an amount (two decimals), a start date, every N days, an end date, and how many claims it
 *  has made so far. */
export type Plan = { id: number; personId: number; amount: string; start: string; every: number; end: string; generated: number };

export function plansKey(sheetId: string) {
  return ['sheet', sheetId, 'plans'];
}

/** The sheet's plans, in the order they were added. */
export function usePlans(sheetId: string) {
  return useQuery({
    queryKey: plansKey(sheetId),
    queryFn: async () => (await getJson<{ plans: Plan[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/plans`)).plans,
  });
}

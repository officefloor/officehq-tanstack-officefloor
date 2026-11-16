import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: a claim of the amount every `every` days from `start` up to `end`, and how many claims it has
 *  made so far. */
export type Plan = { id: number; personId: number; amount: string; start: string; every: number; end: string; generated: number };

export const plansKey = (sheetId: string) => ['sheet', sheetId, 'plans'];

/** Every plan on the sheet, in the order added. */
export function usePlans(sheetId: string) {
  return useQuery({
    queryKey: plansKey(sheetId),
    queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheetId)}/plans`),
  });
}

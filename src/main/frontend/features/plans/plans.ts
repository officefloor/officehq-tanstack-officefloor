import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: an amount (two decimals), from `start` every `every` days to `end` (yyyy-MM-dd), and how many
 *  claims it has `generated` so far. */
export type Plan = { id: number; person: number; amount: string; start: string; every: number; end: string; generated: number };

export const plansKey = (sheet: string) => ['plans', sheet] as const;

export const plansPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/plans`;

/** Every plan on the sheet, in the order added. */
export function usePlans(sheet: string) {
  return useQuery({
    queryKey: plansKey(sheet),
    queryFn: () => getJson<Plan[]>(plansPath(sheet)),
  });
}

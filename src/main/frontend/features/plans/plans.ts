import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: an amount (two decimals), from `start` every `every` days (or, when `day` is set,
 *  monthly on that day of the month) to `end` (yyyy-MM-dd), and how many
 *  claims it has `generated` so far, and its `nextDates`: the first five plan dates after the sheet's
 *  today and on or before the end, leaving out the dates a pause skips; whether it is `paused` now; and its amount
 *  `changes`, oldest first, each as `<date> amount=<amount>` (`amount` is the current one). */
export type Plan = { id: number; person: number; amount: string; start: string; every: number; end: string; generated: number;
  day: number | null; nextDates: string[]; paused: boolean; changes: string[] };

export const plansKey = (sheet: string) => ['plans', sheet] as const;

export const plansPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/plans`;

/** Every plan on the sheet, in the order added. */
export function usePlans(sheet: string) {
  return useQuery({
    queryKey: plansKey(sheet),
    queryFn: () => getJson<Plan[]>(plansPath(sheet)),
  });
}

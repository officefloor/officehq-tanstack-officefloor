import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A plan's pause: from the day it was paused to the day it was resumed (null while still paused). */
export type PlanPause = { from: string; to: string | null };

/** A change of a plan's amount: the day it was made and the new amount (two decimals). */
export type PlanChange = { on: string; amount: string };

/** A person's plan: an amount (two decimals), a start date, every N days or monthly on a day of the month (the other null), an end date, how many claims it
 *  has made so far, whether it is paused now, its pauses in the order made, and its amount changes, oldest first. */
export type Plan = {
  id: number;
  personId: number;
  amount: string;
  start: string;
  every: number | null;
  day: number | null;
  end: string;
  generated: number;
  paused: boolean;
  pauses: PlanPause[];
  changes: PlanChange[];
};

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

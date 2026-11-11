import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: a DRAFT claim of its amount (two decimals) on its start date, then every `every`
 *  days — or, monthly, on day `day` of every month from the start's month (a day beyond the month's
 *  length on its last day, dates before the start left out) — up to its end. `generated` is how many claims it has made so far; `nextDates` its first five
 *  dates after the sheet's today and on or before its end, comma and space separated, dates in a pause
 *  (on or after the day it was paused, before the day it was resumed) left out; `paused` whether it is paused now. */
export type Plan = {
  id: string;
  personId: string;
  amount: string;
  start: string;
  every: number | null;
  day: number | null;
  end: string;
  generated: number;
  nextDates: string;
  paused: boolean;
};

export const plansKey = (sheetId: string) => ['sheet', sheetId, 'plans'] as const;

/** The plans on the sheet, in the order they were added. */
export function usePlans(sheetId: string) {
  return useQuery({
    queryKey: plansKey(sheetId),
    queryFn: async () =>
      (await getJson<{ plans: Plan[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/plans`)).plans,
  });
}

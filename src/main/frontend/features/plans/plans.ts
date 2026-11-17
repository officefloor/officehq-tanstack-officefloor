import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: a claim of the amount every `every` days from `start` up to `end` — or, with a `day` (1 to 31)
 *  instead, on that day of every month (the month's last day when it is shorter) — and how many claims it has made so
 *  far, and its next dates: its first five dates after the sheet's today and on or before `end`, comma and space
 *  separated, leaving out the dates its pauses skip (none while it stays paused) — and whether it is paused. Exactly
 *  one of `every` and `day` is set. */
export type Plan = {
  id: number;
  personId: number;
  amount: string;
  start: string;
  every: number | null;
  day: number | null;
  end: string;
  generated: number;
  nextDates: string;
  paused: boolean;
};

export const plansKey = (sheetId: string) => ['sheet', sheetId, 'plans'];

/** Every plan on the sheet, in the order added. */
export function usePlans(sheetId: string) {
  return useQuery({
    queryKey: plansKey(sheetId),
    queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheetId)}/plans`),
  });
}

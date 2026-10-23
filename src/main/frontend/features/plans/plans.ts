import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: a DRAFT claim of the amount for every plan date (the start, then every N days) up to the end,
 *  made when the plans are generated; `generated` is how many claims it has made so far. A monthly plan has no `every`
 *  but a `day` of the month (1 to 31), falling on the month's last day in a shorter month. */
export type Plan = {
  id: number;
  personId: number;
  amount: string;
  start: string;
  every: number | null;
  day: number | null;
  end: string;
  generated: number;
  /** The first five plan dates after the sheet's today and on or before the end, in order. */
  nextDates: string[];
  /** Whether the plan is paused: a plan date on or after the day it was paused and before the day it is resumed is
   *  skipped, not postponed. */
  paused: boolean;
  /** Its amount changes, oldest first, each as "yyyy-mm-dd amount=0.00"; the amount is the current one. */
  changes: string[];
  /** The descendant sheet the plan makes its claims on, for the person of the same name there; null: its own. */
  sheet: string | null;
};

export const plansKey = (sheetId: string) => ['sheet', sheetId, 'plans'] as const;

/** Every plan on the sheet, in the order they were added. */
export function usePlans(sheetId: string) {
  return useQuery({
    queryKey: plansKey(sheetId),
    queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheetId)}/plans`),
  });
}

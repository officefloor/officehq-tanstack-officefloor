import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** A person's plan: an amount (two decimals) every `every` days, or monthly on `day` of the month (one of them is set),
 *  from `start` to `end`, how many claims it has made, its next five dates after the sheet's today (comma
 *  separated, leaving out the dates a pause skips) whether it is paused now, and its
 *  amount changes, oldest first, each as `<date> amount=<amount>` (`amount` is the current one). */
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
  changes: string[];
  /** The descendant sheet it makes its claims on; blank: this one. */
  sheet: string;
};

/** The query key for the sheet's plans; under sheetKey, so re-loading the sheet re-reads them. */
export const plansKey = (sheet: string) => [...sheetKey(sheet), 'plans'] as const;

/** The sheet's plans, in the order they were added. */
export function usePlans() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: plansKey(sheet),
    queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheet)}/plans`),
    enabled: !!sheet,
  });
}

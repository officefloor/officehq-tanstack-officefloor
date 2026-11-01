import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

/** A plan as the server sends it: its person, amount (two decimals), start, every N days (null when monthly) or day of
 *  the month (null otherwise), end
 *  (yyyy-mm-dd), how many claims it has made so far and its next (up to five) plan dates after the sheet's
 *  today (the paused ones left out), and whether it is paused now. */
export type Plan = {
  id: number;
  personId: number;
  amount: string;
  start: string;
  every: number | null;
  day: number | null;
  end: string;
  generated: number;
  nextDates: string[];
  paused: boolean;
};

export const plansKey = (sheet: string) => ['sheet', sheet, 'plans'] as const;

/** The sheet's plans, in the order added, re-read whenever the sheet is loaded. */
export function usePlans(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(plansKey(sheet), asOf),
    queryFn: () => getJson<Plan[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/plans`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}

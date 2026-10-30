import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: the amount (two decimals) from the start every `every` days, or monthly on `day` of the month (the
 *  other null), up to the end; `generated` is how many claims it has made so far; `nextDates` its first five plan dates after
 *  the sheet's today and on or before the end. */
export type Plan = {
  id: string;
  personId: string;
  amount: string;
  start: string;
  every: number | null;
  day: number | null;
  end: string;
  generated: number;
  nextDates: string[];
};

export const plansKey = (sheet: string) => ['sheet', sheet, 'plans'] as const;

/** The sheet's plans, in the order they were added. */
export function usePlans(sheet: string): Plan[] {
  const { data } = useQuery({
    queryKey: plansKey(sheet),
    queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheet)}/plans`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

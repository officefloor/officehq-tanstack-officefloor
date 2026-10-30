import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: the amount (two decimals) from the start every `every` days up to the end; `generated` is how many
 *  claims it has made so far. */
export type Plan = { id: string; personId: string; amount: string; start: string; every: number; end: string; generated: number };

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

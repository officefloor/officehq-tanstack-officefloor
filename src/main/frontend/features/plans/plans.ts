import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's plan: a DRAFT claim of its amount (two decimals) on its start date, then every `every`
 *  days, up to its end. `generated` is how many claims it has made so far. */
export type Plan = {
  id: string;
  personId: string;
  amount: string;
  start: string;
  every: number;
  end: string;
  generated: number;
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

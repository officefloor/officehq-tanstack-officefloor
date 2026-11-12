import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** A person's plan: a DRAFT claim of `amount` on every plan date, `start` then every `every` days, up to
 *  `end` (yyyy-MM-dd); `generated` is how many claims it has made so far. */
export type Plan = { id: number; personId: number; amount: string; start: string; every: number; end: string; generated: number };

/** Under the sheet's key, so loading the sheet again re-reads its plans. */
export const plansKey = (sheet: string) => [...sheetKey(sheet), 'plans'] as const;

export function usePlans(sheet: string) {
  return useQuery({
    queryKey: plansKey(sheet),
    queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheet)}/plans`),
    enabled: sheet !== '',
  });
}

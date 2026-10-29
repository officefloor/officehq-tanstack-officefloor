import { getJson } from '../../api/http';
import { claimsKey } from '../claims/claimsQuery';

/** A person's plan: `amount` with two decimals, `start` and `end` yyyy-mm-dd, a DRAFT claim every `every` days, or (`every` 0)
 *  on day `day` (1 to 31) of every month; `generated` is how many claims it has made so far. */
export type Plan = { id: number; personId: number; amount: string; start: string; every: number; end: string; generated: number; day: number };

/** Under the claims' key, so whatever re-reads the claims (a generate, a re-load of the sheet) re-reads the plans too. */
export const plansKey = (sheetId: string) => [...claimsKey(sheetId), 'plans'] as const;

/** Every plan on the sheet, in the order added. */
export const plansQuery = (sheetId: string) => ({
  queryKey: plansKey(sheetId),
  queryFn: () => getJson<Plan[]>(`/api/sheets/${encodeURIComponent(sheetId)}/plans`),
});

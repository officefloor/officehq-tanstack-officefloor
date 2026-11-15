import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A person's plan: an amount, a start date, every N days or else a day of the month (1 to 31), an end
 *  date, how many claims it has made so far and (when listed) its next dates: the first five plan dates after
 *  the sheet's today and on or before the end. */
export type Plan = { id: number; sheetId: string; personId: number; amount: string; start: string; every: number | null; day: number | null; end: string; by: string; generated: number; nextDates?: string[] };

export type PlanInput = { amount: string; start: string; every: string; day: string; end: string };

export type GenerateResult = { made: number };

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const plansKey = (sheetId: string) => [...sheetKey(sheetId), 'plans'] as const;

/** The sheet's plans, in the order they were added. */
export function listPlans(sheetId: string): Promise<Plan[]> {
  return getJson<Plan[]>(`${sheetBase(sheetId)}/plans`);
}

/** Give the person a plan; refused (rejects) for an amount the claim form would refuse, an interval that is
 *  not a whole number of at least 1, a day of the month not 1 to 31 (or both or neither given), or an end before the start. */
export function addPlan(sheetId: string, by: string, personId: number, input: PlanInput): Promise<Plan> {
  return postJson<Plan>(`${sheetBase(sheetId)}/people/${personId}/plans?by=${encodeURIComponent(by)}`, input);
}

/** Make the DRAFT claims every plan is due up to its end and the sheet's today, that it has not made yet. */
export function generatePlans(sheetId: string, by: string): Promise<GenerateResult> {
  return postJson<GenerateResult>(`${sheetBase(sheetId)}/generate-plans?by=${encodeURIComponent(by)}`, {});
}

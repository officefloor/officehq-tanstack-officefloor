import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A person's plan: an amount, a start date, every N days or else a day of the month (1 to 31), an end
 *  date, how many claims it has made so far and (when listed) its next dates: the first five plan dates after
 *  the sheet's today and on or before the end, leaving out the days it was paused on), whether it is paused now
 *  and the changes of its amount, oldest first. */
export type PlanChange = { changedOn: string; amount: string };

export type Plan = { id: number; sheetId: string; personId: number; amount: string; start: string; every: number | null; day: number | null; end: string; by: string; generated: number; nextDates?: string[]; paused?: boolean; changes?: PlanChange[]; targetSheet?: string | null };

export type PlanInput = { amount: string; start: string; every: string; day: string; end: string; sheet: string };

export type GenerateResult = { made: number };

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const plansKey = (sheetId: string) => [...sheetKey(sheetId), 'plans'] as const;

/** The sheet's plans, in the order they were added. */
export function listPlans(sheetId: string): Promise<Plan[]> {
  return getJson<Plan[]>(`${sheetBase(sheetId)}/plans`);
}

/** Give the person a plan; refused (rejects) for an amount the claim form would refuse, an interval that is
 *  not a whole number of at least 1, a day of the month not 1 to 31 (or both or neither given), an end before the start, or a sheet that is
 *  not a descendant of this one. A plan naming a sheet makes its claims there, for the person of the same name. */
export function addPlan(sheetId: string, by: string, personId: number, input: PlanInput): Promise<Plan> {
  return postJson<Plan>(`${sheetBase(sheetId)}/people/${personId}/plans?by=${encodeURIComponent(by)}`, input);
}

/** Pause the plan from the sheet's today, or resume it; refused (rejects) for pausing a paused plan or
 *  resuming a running one. A plan date on or after the day paused and before the day resumed is skipped. */
export function pausePlan(sheetId: string, by: string, planId: number, on: boolean): Promise<Plan> {
  return postJson<Plan>(`${sheetBase(sheetId)}/plans/${planId}/${on ? 'pause' : 'resume'}?by=${encodeURIComponent(by)}`, {});
}

/** Change the plan's amount from the sheet's today; refused (rejects) for an amount the claim form would refuse
 *  (or negative). The claims it makes from then on are of the new amount; those already made keep theirs. */
export function changePlanAmount(sheetId: string, by: string, planId: number, amount: string): Promise<Plan> {
  return postJson<Plan>(`${sheetBase(sheetId)}/plans/${planId}/amount?by=${encodeURIComponent(by)}`, { amount });
}

/** Make the DRAFT claims every plan is due up to its end and the sheet's today, that it has not made yet. */
export function generatePlans(sheetId: string, by: string): Promise<GenerateResult> {
  return postJson<GenerateResult>(`${sheetBase(sheetId)}/generate-plans?by=${encodeURIComponent(by)}`, {});
}

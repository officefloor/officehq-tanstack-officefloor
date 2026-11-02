import { counts, isOpen, useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { rateFor, sumDues, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

export type Aging = { d0to30: string; d31to60: string; d61: string };

/** A claim's age on the sheet's today: the days from its date; 0 for a claim dated after today. */
export function ageDays(date: string, today: string): number {
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${date}T00:00:00Z`)) / 86_400_000;
  return Math.max(0, Math.round(days));
}

/** Each person's aging, by person id: the sum of the dues now of their OPEN and PENDING claims that count,
 *  split by the claim's age (0 to 30 days, 31 to 60, 61 or more), skipping those with none.
 *  Empty until the sheet's today is loaded. */
export function useAging(sheetId: string): Map<number, Aging> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  const holds = holdsById(people.data, today);
  const aging = new Map<number, Aging>();
  if (today === undefined) return aging;
  for (const p of people.data ?? []) {
    const buckets: [string[], string[], string[]] = [[], [], []];
    for (const c of claims.data ?? []) {
      if (c.personId !== p.id || !isOpen(c) || !counts(c, holds, today)) continue;
      const age = ageDays(c.date, today);
      const d = claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(p, c.date), c.date), settings, today);
      buckets[age <= 30 ? 0 : age <= 60 ? 1 : 2].push(d);
    }
    aging.set(p.id, { d0to30: sumDues(buckets[0]), d31to60: sumDues(buckets[1]), d61: sumDues(buckets[2]) });
  }
  return aging;
}

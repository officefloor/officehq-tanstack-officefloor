import { PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { gradeOn } from '../people/people';
import { claimDue, rateOn, sumDue, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { live, unsettled, useClaims } from './claims';

const DAY_MS = 24 * 60 * 60 * 1000;

/** A claim's age on the sheet's today: the days from its date (negative when dated after today). */
function ageOf(date: string, today: string): number {
  return Math.round((Date.parse(`${today}T00:00:00Z`) - Date.parse(`${date}T00:00:00Z`)) / DAY_MS);
}

/** A person's aged dues: the due (as shown on its row) of their OPEN and PENDING claims that count in the live
 *  totals, added up by the claim's age on the sheet's today — 0 to 30 days (a claim dated after today included),
 *  31 to 60, and 61 or more — skipping n/a. */
function PersonAged({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const buckets: [string[], string[], string[]] = [[], [], []];
  if (today !== '') {
    for (const c of live(claims, [person], today).filter(unsettled)) {
      const age = ageOf(c.date, today);
      const due = claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date, c.id), floor, late);
      buckets[age <= 30 ? 0 : age <= 60 ? 1 : 2].push(due);
    }
  }
  return (
    <>
      <td data-testid="person-aged-0-30">{sumDue(buckets[0])}</td>
      <td data-testid="person-aged-31-60">{sumDue(buckets[1])}</td>
      <td data-testid="person-aged-61">{sumDue(buckets[2])}</td>
    </>
  );
}

export const contribution = PersonRow.fill({ order: 26, Component: PersonAged });

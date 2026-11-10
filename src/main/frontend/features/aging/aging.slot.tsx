import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate } from '../access/access';
import { personAged } from './aging';

function AgedHead() {
  return (
    <>
      <th>Due 0–30 days</th>
      <th>Due 31–60 days</th>
      <th>Due 61+ days</th>
    </>
  );
}

// A person's open claims that count: their due split by the claim's age on the sheet's today.
function PersonAged({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const aged = personAged(rates, person.grades, today, claims, liveHolds(person, today), floor, late);
  return (
    <>
      <td data-testid="person-aged-0-30">{aged.upTo30}</td>
      <td data-testid="person-aged-31-60">{aged.upTo60}</td>
      <td data-testid="person-aged-61">{aged.over60}</td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 24, Component: AgedHead }),
  PersonRow.fill({ order: 24, Component: PersonAged }),
];

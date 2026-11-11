import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate } from '../access/access';
import { sumDue } from '../rates/rates';
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

// The sheet's due by age: everyone's age buckets simply added up (not trimmed).
function SheetAged({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const each = people.map((p) =>
    personAged(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late),
  );
  return (
    <p>
      Due 0–30 days in all: <strong data-testid="rollup-aged-0-30">{sumDue(each.map((a) => a.upTo30))}</strong>, 31–60
      days: <strong data-testid="rollup-aged-31-60">{sumDue(each.map((a) => a.upTo60))}</strong>, 61+ days:{' '}
      <strong data-testid="rollup-aged-61">{sumDue(each.map((a) => a.over60))}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 24, Component: SheetAged }),
  PersonHead.fill({ order: 24, Component: AgedHead }),
  PersonRow.fill({ order: 24, Component: PersonAged }),
];

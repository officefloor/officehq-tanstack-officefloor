import { SheetPage } from '../../slots/defs/sheetPage';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { live, useClaims } from '../claims/claims';
import { usePeople, type Person } from '../people/people';
import { sum, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { aged } from './aging';

function AgingHead() {
  return (
    <>
      <th>Due 0–30</th>
      <th>Due 31–60</th>
      <th>Due 61+</th>
    </>
  );
}

/** A person's due on their open claims that count, split by the claim's age (see aged). */
function PersonAging({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const mine = live(claims.data?.filter((c) => c.personId === person.id) ?? [], [person], late.today);
  const a = aged(mine, rates.data, person, late, floor);
  return (
    <>
      <td data-testid="person-aged-0-30">{a.d30}</td>
      <td data-testid="person-aged-31-60">{a.d60}</td>
      <td data-testid="person-aged-61">{a.d61}</td>
    </>
  );
}

/** The sheet's due by age: everyone's age buckets added up, skipping n/a, with no trimming. */
function SheetAging({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const all = (people.data ?? []).map((p) =>
    aged(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor),
  );
  return (
    <p>
      Due 0–30 <span data-testid="rollup-aged-0-30">{sum(all.map((a) => a.d30))}</span>
      {' '}Due 31–60 <span data-testid="rollup-aged-31-60">{sum(all.map((a) => a.d60))}</span>
      {' '}Due 61+ <span data-testid="rollup-aged-61">{sum(all.map((a) => a.d61))}</span>
    </p>
  );
}

export const contributions = [
  SheetPage.fill({ order: 30.5, Component: SheetAging }),
  PeopleHead.fill({ order: 21, Component: AgingHead }),
  PersonRow.fill({ order: 21, Component: PersonAging }),
];

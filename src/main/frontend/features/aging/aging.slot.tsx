import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { live, useClaims } from '../claims/claims';
import { type Person } from '../people/people';
import { useRates } from '../rates/rates';
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

export const contributions = [
  PeopleHead.fill({ order: 21, Component: AgingHead }),
  PersonRow.fill({ order: 21, Component: PersonAging }),
];

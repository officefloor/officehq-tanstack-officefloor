import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { claimed, live, useClaims } from './claims';

// A person's claimed (the sum of their claims) and the sheet's claimed-total (everyone's).
function ClaimedHead() {
  return <th>Claimed</th>;
}

function PersonClaimed({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  return <td data-testid="person-claimed">{claimed(live((claims.data ?? []).filter((c) => c.personId === person.id), [person]))}</td>;
}

function ClaimedTotal() {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  return (
    <p>
      Claimed total <strong data-testid="rollup-claimed-total">{claimed(live(claims.data ?? [], people.data))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 10, Component: ClaimedHead }),
  PersonColumns.fill({ order: 10, Component: PersonClaimed }),
  HomeMain.fill({ order: 30, Component: ClaimedTotal }),
];

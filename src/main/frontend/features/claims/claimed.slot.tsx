import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { claimed, live, useClaims } from './claims';
import { useFigure } from '../locale/locale';

// A person's claimed (the sum of their claims, leaving out corrections) and the sheet's claimed-total (everyone's).
function ClaimedHead() {
  return <th>Claimed</th>;
}

function PersonClaimed({ person }: { person: Person }) {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  return <td data-testid="person-claimed">{fig(claimed(live((claims.data ?? []).filter((c) => c.personId === person.id), [person])))}</td>;
}

function ClaimedTotal() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  return (
    <p>
      Claimed total <strong data-testid="rollup-claimed-total">{fig(claimed(live(claims.data ?? [], people.data)))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 10, Component: ClaimedHead }),
  PersonColumns.fill({ order: 10, Component: PersonClaimed }),
  HomeMain.fill({ order: 30, Component: ClaimedTotal }),
];

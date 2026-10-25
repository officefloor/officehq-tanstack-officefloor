import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople, type Person } from '../people/people';
import { trimmedSum } from '../rates/rates';
import { cutTotalOf, usePools } from './pools';

// A person's cut total: their cuts across every pool they are a member of, added up.
function CutTotalHeader() {
  return <th>Cut total</th>;
}

function PersonCutTotal({ person }: { person: Person }) {
  const pools = usePools();
  return <td data-testid="person-cut-total">{cutTotalOf(pools.data ?? [], person.id)}</td>;
}

// The sheet's cut total: everyone's cut total added up, trimmed (the single highest and lowest person's figure left out
// when three or more have one).
function CutTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const pools = usePools();
  if (!user || !sheet) return null;
  return (
    <p>
      Cut in total <strong data-testid="rollup-cut-total">{trimmedSum((people.data ?? []).map((p) => cutTotalOf(pools.data ?? [], p.id)))}</strong>
    </p>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 10.5, Component: CutTotalHeader }),
  PersonCells.fill({ order: 10.5, Component: PersonCutTotal }),
  HomeMain.fill({ order: 30.5, Component: CutTotal }),
];

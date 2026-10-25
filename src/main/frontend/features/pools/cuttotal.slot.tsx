import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Person } from '../people/people';
import { cutTotalOf, usePools } from './pools';

// A person's cut total: their cuts across every pool they are a member of, added up.
function CutTotalHeader() {
  return <th>Cut total</th>;
}

function PersonCutTotal({ person }: { person: Person }) {
  const pools = usePools();
  return <td data-testid="person-cut-total">{cutTotalOf(pools.data ?? [], person.id)}</td>;
}

// The sheet's cut total: everyone's cut total added up.
function CutTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const pools = usePools();
  if (!user || !sheet) return null;
  return (
    <p>
      Cut in total <strong data-testid="rollup-cut-total">{cutTotalOf(pools.data ?? [], null)}</strong>
    </p>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 10.5, Component: CutTotalHeader }),
  PersonCells.fill({ order: 10.5, Component: PersonCutTotal }),
  HomeMain.fill({ order: 30.5, Component: CutTotal }),
];

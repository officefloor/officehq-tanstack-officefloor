import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet } from '../../session/session';
import { type Person, usePeople } from '../people/people';
import { dueTotal } from '../rates/rates';
import { cutTotal, usePools } from './pools';
import { useFigure } from '../locale/locale';

// A person's cut total (see `cutTotal`) and the sheet's cut total (everyone's added up).

function CutHead() {
  return <th>Cut total</th>;
}

function PersonCut({ person }: { person: Person }) {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const pools = usePools(sheet);
  return <td data-testid="person-cut-total">{fig(cutTotal(pools.data ?? [], person.id))}</td>;
}

function CutTotal() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const pools = usePools(sheet);
  return (
    <p>
      Cut total{' '}
      <strong data-testid="rollup-cut-total">{fig(dueTotal((people.data ?? []).map((p) => cutTotal(pools.data ?? [], p.id))))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.96, Component: CutHead }),
  PersonColumns.fill({ order: 19.96, Component: PersonCut }),
  HomeMain.fill({ order: 30.5, Component: CutTotal }),
];

import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet } from '../../session/session';
import { type Person, usePeople } from '../people/people';
import { dueTotal } from '../rates/rates';
import { type Pool, usePools } from './pools';

// A person's cut total (their cuts across every pool they are a member of, a pool showing no cut adding
// nothing) and the sheet's cut total (everyone's added up).
const cutTotal = (pools: Pool[], personId: number) =>
  dueTotal(pools.flatMap((p) => p.members.filter((m) => m.personId === personId && m.cut !== null).map((m) => m.cut as string)));

function CutHead() {
  return <th>Cut total</th>;
}

function PersonCut({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const pools = usePools(sheet);
  return <td data-testid="person-cut-total">{cutTotal(pools.data ?? [], person.id)}</td>;
}

function CutTotal() {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const pools = usePools(sheet);
  return (
    <p>
      Cut total{' '}
      <strong data-testid="rollup-cut-total">{dueTotal((people.data ?? []).map((p) => cutTotal(pools.data ?? [], p.id)))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.96, Component: CutHead }),
  PersonColumns.fill({ order: 19.96, Component: PersonCut }),
  HomeMain.fill({ order: 30.5, Component: CutTotal }),
];

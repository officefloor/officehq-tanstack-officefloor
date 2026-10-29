import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { sumDue } from '../rates/ratesQuery';
import { cutTotal, poolsQuery } from './poolsQuery';

function PersonCutTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const pools = useQuery(poolsQuery(sheetId)).data ?? [];
  return <td data-testid="person-cut-total">{cutTotal(pools, personId)}</td>;
}

/** The sheet's cut total: everyone's cut total added up. */
function CutTotal({ sheetId }: { sheetId: string }) {
  const pools = useQuery(poolsQuery(sheetId)).data ?? [];
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  return (
    <p data-testid="rollup-cut">
      Cut total <strong data-testid="rollup-cut-total">{sumDue(people.map((p) => cutTotal(pools, p.id)))}</strong>
    </p>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 22.5, Component: () => <th>Cuts</th> }),
  PersonCells.fill({ order: 22.5, Component: PersonCutTotal }),
  SheetPanel.fill({ order: 25.6, Component: CutTotal }),
];

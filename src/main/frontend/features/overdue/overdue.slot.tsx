import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { trimmedSum } from '../rates/useRates';
import { useOverdue } from './useOverdue';

/** A person's overdue count and overdue-total: their claims that count and are overdue, and the sum
 *  of those claims' figures. */
function PersonOverdue({ sheetId, personId }: { sheetId: string; personId: number }) {
  const figures = useOverdue(sheetId).get(personId);
  return (
    <>
      <td data-testid="person-overdue-count">{figures?.count ?? ''}</td>
      <td data-testid="person-overdue-total">{figures?.total ?? ''}</td>
    </>
  );
}

/** The sheet's overdue count and overdue-total: everyone's added up, leaving out the
 *  highest and the lowest overdue-total when three or more people have one. */
function SheetOverdue({ sheetId }: { sheetId: string }) {
  const figures = [...useOverdue(sheetId).values()];
  return (
    <p>
      Overdue: <strong data-testid="rollup-overdue-count">{figures.reduce((n, f) => n + f.count, 0)}</strong>,
      total <strong data-testid="rollup-overdue-total">{trimmedSum(figures.map((f) => f.total))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 25, Component: () => (
    <>
      <th>Overdue</th>
      <th>Overdue total</th>
    </>
  ) }),
  PersonCells.fill({ order: 25, Component: PersonOverdue }),
  SheetBody.fill({ order: 35, Component: ({ sheetId }) => <SheetOverdue sheetId={sheetId} /> }),
];

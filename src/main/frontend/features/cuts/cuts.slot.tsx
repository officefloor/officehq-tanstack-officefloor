import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { sumDues } from '../rates/useRates';
import { useCutTotals } from './useCutTotals';
import { useFigure } from '../locale/useLocale';

/** A person's cut total: their cuts across every pool they are a member of, added up. */
function PersonCutTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const totals = useCutTotals(sheetId);
  return <td data-testid="person-cut-total">{fig(totals.get(personId) ?? '')}</td>;
}

/** The sheet's cut total: everyone's cut total added up. */
function CutTotal({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const totals = useCutTotals(sheetId);
  return (
    <p>
      Cut total: <strong data-testid="rollup-cut-total">{fig(sumDues([...totals.values()]))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 24, Component: () => <th>Cut</th> }),
  PersonCells.fill({ order: 24, Component: PersonCutTotal }),
  SheetBody.fill({ order: 32, Component: ({ sheetId }) => <CutTotal sheetId={sheetId} /> }),
];

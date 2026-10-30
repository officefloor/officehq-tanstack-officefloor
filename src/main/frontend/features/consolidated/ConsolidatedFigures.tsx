import { LiveReads } from '../asof/useAsOf';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSheetFigures } from '../people/useSheetFigures';
import { formatCents, toCents } from '../rates/due';
import { useDescendants } from './useDescendants';

type Totals = { people: number; due: bigint; overdue: bigint };

// The figures over the sheet and all its descendants: their people, and each sheet's due-total and
// overdue-total as that sheet shows them, added up. A sheet with no children shows its own. They are
// the live figures, also while the screen is shown as of a date.
export function ConsolidatedFigures() {
  const [sheet] = useSearchParam('sheet', asString);
  const descendants = useDescendants(sheet);
  return (
    <LiveReads.Provider value={true}>
      <Add sheets={[sheet, ...descendants]} totals={{ people: 0, due: 0n, overdue: 0n }} />
    </LiveReads.Provider>
  );
}

// Adds the first sheet's figures to the totals and passes on to the rest: one sheet's hooks per
// component, so the number of sheets can change.
function Add({ sheets, totals }: { sheets: string[]; totals: Totals }) {
  if (sheets.length === 0) return <Shown totals={totals} />;
  return <AddSheet key={sheets[0]} sheets={sheets} totals={totals} />;
}

function AddSheet({ sheets, totals }: { sheets: string[]; totals: Totals }) {
  const { people, sheetDueTotal, sheetOverdueTotal } = useSheetFigures(sheets[0]);
  return (
    <Add
      sheets={sheets.slice(1)}
      totals={{
        people: totals.people + people.length,
        due: totals.due + toCents(sheetDueTotal),
        overdue: totals.overdue + toCents(sheetOverdueTotal),
      }}
    />
  );
}

function Shown({ totals }: { totals: Totals }) {
  return (
    <section data-testid="consolidated">
      Consolidated — People: <span data-testid="consolidated-people-count">{totals.people}</span>
      {' · '}Due: <span data-testid="consolidated-due-total">{formatCents(totals.due)}</span>
      {' · '}Overdue: <span data-testid="consolidated-overdue-total">{formatCents(totals.overdue)}</span>
    </section>
  );
}

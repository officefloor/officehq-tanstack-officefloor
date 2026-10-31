import { asString, useSearchParam } from '../../url/useSearchParam';
import { GRADES, gradeOn } from '../people/usePeople';
import { STATUSES, useSheetFigures } from '../people/useSheetFigures';
import { formatCents, toCents } from '../rates/due';
import { useDescendants } from './useDescendants';

type Totals = { people: number; due: bigint; overdue: bigint; grades: bigint[]; statuses: number[] };

// The figures over the sheet and all its descendants: their people, and each sheet's due-total and
// overdue-total as that sheet shows them, added up. Per grade, the due-totals of the people on it
// today (retired people left out, n/a skipped) as each sheet's summary shows them, added up; per
// status, how many claims have it, added up. A sheet with no children shows its own. While the
// screen is shown as of a date, so are they: every sheet of the tree as it stood at the end of that day,
// with that date as its today.
export function ConsolidatedFigures() {
  const [sheet] = useSearchParam('sheet', asString);
  const descendants = useDescendants(sheet);
  return (
    <Add sheets={[sheet, ...descendants]} totals={{ people: 0, due: 0n, overdue: 0n, grades: GRADES.map(() => 0n), statuses: STATUSES.map(() => 0) }} />
  );
}

// Adds the first sheet's figures to the totals and passes on to the rest: one sheet's hooks per
// component, so the number of sheets can change.
function Add({ sheets, totals }: { sheets: string[]; totals: Totals }) {
  if (sheets.length === 0) return <Shown totals={totals} />;
  return <AddSheet key={sheets[0]} sheets={sheets} totals={totals} />;
}

function AddSheet({ sheets, totals }: { sheets: string[]; totals: Totals }) {
  const { people, today, totalOf, statusCount, sheetDueTotal, sheetOverdueTotal } = useSheetFigures(sheets[0]);
  const current = people.filter((p) => !p.retired);
  const gradeDue = (g: (typeof GRADES)[number]) =>
    current.filter((p) => gradeOn(p, today) === g).reduce((sum, p) => sum + (totalOf(p) ?? 0n), 0n);
  return (
    <Add
      sheets={sheets.slice(1)}
      totals={{
        people: totals.people + people.length,
        due: totals.due + toCents(sheetDueTotal),
        overdue: totals.overdue + toCents(sheetOverdueTotal),
        grades: GRADES.map((g, i) => totals.grades[i] + gradeDue(g)),
        statuses: STATUSES.map((s, i) => totals.statuses[i] + statusCount(s)),
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
      {GRADES.map((g, i) => (
        <span key={g}>
          {' · '}Grade {g}: <span data-testid={`consolidated-grade-${g}-due-total`}>{formatCents(totals.grades[i])}</span>
        </span>
      ))}
      {STATUSES.map((s, i) => (
        <span key={s}>
          {' · '}{s}: <span data-testid={`consolidated-status-${s}-count`}>{totals.statuses[i]}</span>
        </span>
      ))}
    </section>
  );
}

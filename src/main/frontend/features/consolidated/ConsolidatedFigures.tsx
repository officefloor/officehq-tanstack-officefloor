import { asString, useSearchParam } from '../../url/useSearchParam';
import { useGradeCodes } from '../grades/useGrades';
import { gradeOn } from '../people/usePeople';
import { STATUSES, trimmedSum, useSheetFigures } from '../people/useSheetFigures';
import { formatCents } from '../rates/due';
import { useDescendants } from './useDescendants';

type Totals = { people: string[][]; due: bigint[]; overdue: bigint[]; grades: bigint[]; statuses: number[] };

// The root sheet's grades, the order of Totals.grades.
type Grades = string[];

// The figures over the sheet and all its descendants: their people, each counted once across the
// tree (people whose names or aliases match, as names are compared, are one person); the due-total
// and overdue-total, every person's figure over the tree (n/a skipped for due) added up, leaving out
// the single highest and the single lowest when there are three or more. Per grade, the due-totals of the people on it
// today (retired people left out, n/a skipped) as each sheet's summary shows them, added up; per
// status, how many claims have it, added up. A sheet with no children shows its own. While the
// screen is shown as of a date, so are they: every sheet of the tree as it stood at the end of that day,
// with that date as its today.
export function ConsolidatedFigures() {
  const [sheet] = useSearchParam('sheet', asString);
  const descendants = useDescendants(sheet);
  const grades = useGradeCodes(sheet);
  return (
    <Add sheets={[sheet, ...descendants]} grades={grades} totals={{ people: [], due: [], overdue: [], grades: grades.map(() => 0n), statuses: STATUSES.map(() => 0) }} />
  );
}

// Adds the first sheet's figures to the totals and passes on to the rest: one sheet's hooks per
// component, so the number of sheets can change.
function Add({ sheets, grades, totals }: { sheets: string[]; grades: Grades; totals: Totals }) {
  if (sheets.length === 0) return <Shown grades={grades} totals={totals} />;
  return <AddSheet key={sheets[0]} sheets={sheets} grades={grades} totals={totals} />;
}

function AddSheet({ sheets, grades, totals }: { sheets: string[]; grades: Grades; totals: Totals }) {
  const { people, today, totalOf, statusCount, dueFigures, overdueFigures } = useSheetFigures(sheets[0]);
  const current = people.filter((p) => !p.retired);
  const gradeDue = (g: string) =>
    current.filter((p) => gradeOn(p, today) === g).reduce((sum, p) => sum + (totalOf(p) ?? 0n), 0n);
  return (
    <Add
      sheets={sheets.slice(1)}
      grades={grades}
      totals={{
        people: [...totals.people, ...people.map((p) => [p.name, ...p.aliases].map(nameKey))],
        due: [...totals.due, ...dueFigures],
        overdue: [...totals.overdue, ...overdueFigures],
        grades: grades.map((g, i) => (totals.grades[i] ?? 0n) + gradeDue(g)),
        statuses: STATUSES.map((s, i) => totals.statuses[i] + statusCount(s)),
      }}
    />
  );
}

// A name as names are compared: without regard to case or outer spaces.
const nameKey = (name: string) => name.trim().toLowerCase();

// How many people there are when those sharing a name or alias (directly or through others) are one.
function distinctPeople(people: string[][]): number {
  const parent = people.map((_, i) => i);
  const find = (i: number): number => (parent[i] === i ? i : (parent[i] = find(parent[i])));
  const holder = new Map<string, number>();
  people.forEach((keys, i) =>
    keys.forEach((k) => {
      const j = holder.get(k);
      if (j === undefined) holder.set(k, i);
      else parent[find(i)] = find(j);
    }),
  );
  return people.filter((_, i) => find(i) === i).length;
}

function Shown({ grades, totals }: { grades: Grades; totals: Totals }) {
  return (
    <section data-testid="consolidated">
      Consolidated — People: <span data-testid="consolidated-people-count">{distinctPeople(totals.people)}</span>
      {' · '}Due: <span data-testid="consolidated-due-total">{formatCents(trimmedSum(totals.due))}</span>
      {' · '}Overdue: <span data-testid="consolidated-overdue-total">{formatCents(trimmedSum(totals.overdue))}</span>
      {grades.map((g, i) => (
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

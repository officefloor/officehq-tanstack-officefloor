import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet } from '../../session/session';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { STATUSES, statusCount, useClaims } from '../claims/claims';
import { useDescendants, useSheetToday } from '../sheet/sheet';
import { dueTotal, trimmedTotal } from '../rates/rates';
import { usePersonDue } from '../rates/due.slot';
import { useOverdue } from '../overdue/overdue.slot';
import { useFigure } from '../locale/locale';

type Figures = {
  people: string[][];
  due: string[];
  overdue: string[];
  gradeDue: Record<string, string[]>;
  statuses: Record<string, number>;
};

const noFigures = (): Figures => ({
  people: [],
  due: [],
  overdue: [],
  gradeDue: Object.fromEntries(GRADES.map((g) => [g, []])),
  statuses: Object.fromEntries(STATUSES.map((s) => [s, 0])),
});

/** Names are compared ignoring case and the spaces around them. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** How many people there are across the tree, given each one's name and aliases (as names are compared): people
 *  whose names or aliases match, directly or through others, are one person. */
function distinctPeople(names: string[][]): number {
  const parent = names.map((_, i) => i);
  const root = (i: number): number => (parent[i] === i ? i : (parent[i] = root(parent[i])));
  const owner = new Map<string, number>();
  names.forEach((keys, i) =>
    keys.filter((k) => k !== '').forEach((k) => {
      const j = owner.get(k);
      if (j === undefined) owner.set(k, i);
      else parent[root(i)] = root(j);
    }),
  );
  return names.filter((_, i) => root(i) === i).length;
}

/** Adds the first sheet's figures (as that sheet shows them) to those so far, then the rest's, one sheet per
 *  component so each reads its sheet with the same hooks; shows the sums once every sheet is added. */
function Consolidate({ sheets, sofar }: { sheets: string[]; sofar: Figures }) {
  const fig = useFigure();
  const [first, ...rest] = sheets;
  const people = usePeople(first);
  const overdueOf = useOverdue(first);
  const claims = useClaims(first);
  const today = useSheetToday(first);
  const personDue = usePersonDue(first);
  const active = (people.data ?? []).filter((p) => !p.retired);
  const figures: Figures = {
    people: [...sofar.people, ...(people.data ?? []).map((p) => [p.name, ...p.aliases].map(nameKey))],
    due: [...sofar.due, ...(people.data ?? []).map(personDue)],
    overdue: [...sofar.overdue, ...(people.data ?? []).map((p) => overdueOf(p).total)],
    gradeDue: Object.fromEntries(
      GRADES.map((g) => [g, [...sofar.gradeDue[g], ...active.filter((p) => gradeOn(p.grades, today) === g).map(personDue)]]),
    ),
    statuses: Object.fromEntries(STATUSES.map((s) => [s, sofar.statuses[s] + statusCount(claims.data, s)])),
  };
  if (rest.length > 0) return <Consolidate sheets={rest} sofar={figures} />;
  return (
    <>
      People <strong data-testid="consolidated-people-count">{distinctPeople(figures.people)}</strong>{' '}
      Due total <strong data-testid="consolidated-due-total">{fig(trimmedTotal(figures.due))}</strong>{' '}
      Overdue total <strong data-testid="consolidated-overdue-total">{fig(trimmedTotal(figures.overdue))}</strong>
      {GRADES.map((g) => (
        <span key={g}>
          {' '}Grade {g} due total{' '}
          <strong data-testid={`consolidated-grade-${g}-due-total`}>{fig(dueTotal(figures.gradeDue[g]))}</strong>
        </span>
      ))}
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}{s} <strong data-testid={`consolidated-status-${s}-count`}>{figures.statuses[s]}</strong>
        </span>
      ))}
    </>
  );
}

// The consolidated figures over the sheet and all its descendants: their people, each counted once across the
// tree (people whose names or aliases match are one person); the due- and overdue-totals of every person of
// the tree added up, skipping n/a, trimmed once of the single highest and lowest when three or more have one; per grade, the due-totals of the people on it on their sheet's today
// (retired people left out) added up; and per status, how many claims have it. While the screen has an as-of date, every sheet of the tree is read
// as it stood at the end of that day, with that date as its today. A sheet with no children shows its own.
function ConsolidatedFigures() {
  const [sheet] = useCurrentSheet();
  const descendants = useDescendants(sheet);
  if (sheet === '' || !descendants.data) return null;
  return (
    <p data-testid="consolidated-figures">
      Consolidated{' '}
      <Consolidate key={descendants.data.join(',')} sheets={[sheet, ...descendants.data]} sofar={noFigures()} />
    </p>
  );
}

export const contribution = AppHeader.fill({ order: 31, Component: ConsolidatedFigures });

import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { useGrades } from '../grades/gradesQuery';
import { useGradeDueTotals, usePeopleDueTotals, usePeopleSettledOpen } from '../rates/due.slot';
import { usePeopleCutTotals } from '../pools/cutTotal.slot';
import { usePeopleOverdueTotals } from '../rates/overdue.slot';
import { sumDue, trimmedSum } from '../rates/ratesQuery';
import { STATUSES, statusCount } from '../summary/summary.slot';
import { descendantsQuery } from './consolidatedQuery';

/**
 * `names` holds each person's name and aliases (as names are compared); `dues` and `overdues` each person's due-total
 * and overdue-total, `settleds`, `opens` and `cuts` each person's settled-total, open-total and cut total, all over the sheets so far. `grades` follows `codes` (the grades of the sheet the tree is shown from) and `statuses` follows `STATUSES`.
 */
type Figures = { names: string[][]; dues: string[]; overdues: string[]; settleds: string[]; opens: string[]; cuts: string[]; codes: string[]; grades: string[]; statuses: number[] };

const nameKey = (name: string) => name.trim().toLowerCase();

/** How many people there are when those whose names or aliases match (as names are compared) are one person. */
function distinctPeople(names: string[][]): number {
  const parent = names.map((_, i) => i);
  const root = (i: number): number => (parent[i] === i ? i : (parent[i] = root(parent[i])));
  const owner = new Map<string, number>();
  names.forEach((keys, i) =>
    keys.forEach((k) => {
      const j = owner.get(k);
      if (j === undefined) owner.set(k, i);
      else parent[root(i)] = root(j);
    }),
  );
  return names.filter((_, i) => root(i) === i).length;
}

/**
 * Adds the figures of the first sheet in `sheetIds` — its people's, as their rows show them, and its header's — to
 * `sum`, then hands on to the rest; the last shows the consolidated figures. One component per sheet keeps each sheet's hooks fixed.
 */
function Accumulate({ sheetIds, sum }: { sheetIds: string[]; sum: Figures }) {
  const [sheetId, ...rest] = sheetIds;
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const dueByPerson = usePeopleDueTotals(sheetId);
  const overdues = usePeopleOverdueTotals(sheetId);
  const grades = useGradeDueTotals(sheetId, sum.codes);
  const settledOpen = usePeopleSettledOpen(sheetId);
  const cuts = usePeopleCutTotals(sheetId);
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const next = {
    codes: sum.codes,
    names: [...sum.names, ...people.map((p) => [p.name, ...p.aliases].map(nameKey))],
    dues: [...sum.dues, ...people.map((p) => dueByPerson.get(p.id) ?? 'n/a')],
    overdues: [...sum.overdues, ...overdues],
    settleds: [...sum.settleds, ...settledOpen.map(([s]) => s)],
    opens: [...sum.opens, ...settledOpen.map(([, o]) => o)],
    cuts: [...sum.cuts, ...cuts],
    grades: grades.map((g, i) => sumDue([sum.grades[i], g])),
    statuses: STATUSES.map((s, i) => sum.statuses[i] + statusCount(claims, s)),
  };
  if (rest.length > 0) return <Accumulate key={rest[0]} sheetIds={rest} sum={next} />;
  // The trim applies once, over every person of the tree.
  const due = trimmedSum(next.dues);
  const overdue = trimmedSum(next.overdues);
  const settled = trimmedSum(next.settleds);
  const open = trimmedSum(next.opens);
  const cut = trimmedSum(next.cuts);
  return (
    <p data-testid="consolidated">
      Consolidated: people <strong data-testid="consolidated-people-count">{distinctPeople(next.names)}</strong>
      {' '}· Due <strong data-testid="consolidated-due-total">{due}</strong>
      {' '}· Overdue <strong data-testid="consolidated-overdue-total">{overdue}</strong>
      {next.codes.map((g, i) => (
        <span key={g}>
          {' '}· {g} due <strong data-testid={`consolidated-grade-${g}-due-total`}>{next.grades[i]}</strong>
        </span>
      ))}
      {STATUSES.map((s, i) => (
        <span key={s}>
          {' '}· {s} <strong data-testid={`consolidated-status-${s}-count`}>{next.statuses[i]}</strong>
        </span>
      ))}
      {' '}· Settled <strong data-testid="consolidated-settled-total">{settled}</strong>
      {' '}· Open <strong data-testid="consolidated-open-total">{open}</strong>
      {' '}· Cut total <strong data-testid="consolidated-cut-total">{cut}</strong>
    </p>
  );
}

/**
 * The sheet's figures added up with those of all its descendants: people, each counted once across the tree (those
 * whose names or aliases match are one person); due-total, overdue-total, settled-total, open-total and cut total, over every person of the tree trimmed
 * once of the single highest and lowest; per grade, the
 * due-totals of the people on it (retired people left out); per status, the count of claims in it. A sheet with no
 * children shows its own. While the screen is shown as of a date, so is every sheet of the tree: as it stood at the end
 * of that day, with that date as its today (the tree itself is the one named now).
 */
function Consolidated({ sheetId }: { sheetId: string }) {
  const descendants = useQuery(descendantsQuery(sheetId)).data ?? [];
  const codes = useGrades(sheetId);
  const sheetIds = [sheetId, ...descendants];
  return <Accumulate key={sheetIds.join('\n')} sheetIds={sheetIds} sum={{ codes, names: [], dues: [], overdues: [], settleds: [], opens: [], cuts: [], grades: codes.map(() => '0.00'), statuses: STATUSES.map(() => 0) }} />;
}

export const contribution = SheetPanel.fill({ order: -9, Component: Consolidated });

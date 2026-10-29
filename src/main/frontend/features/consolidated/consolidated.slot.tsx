import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { GRADES, peopleQuery } from '../people/peopleQuery';
import { useGradeDueTotals, useSheetDueTotal } from '../rates/due.slot';
import { useSheetOverdue } from '../rates/overdue.slot';
import { sumDue } from '../rates/ratesQuery';
import { STATUSES, statusCount } from '../summary/summary.slot';
import { descendantsQuery } from './consolidatedQuery';

/** `grades` follows `GRADES` and `statuses` follows `STATUSES`. */
type Figures = { people: number; due: string; overdue: string; grades: string[]; statuses: number[] };

/**
 * Adds the figures of the first sheet in `sheetIds` — as that sheet shows them in its header — to `sum`, then hands on
 * to the rest; the last shows the consolidated figures. One component per sheet keeps each sheet's hooks fixed.
 */
function Accumulate({ sheetIds, sum }: { sheetIds: string[]; sum: Figures }) {
  const [sheetId, ...rest] = sheetIds;
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const due = useSheetDueTotal(sheetId);
  const { total: overdue } = useSheetOverdue(sheetId);
  const grades = useGradeDueTotals(sheetId);
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const next = {
    people: sum.people + people.length,
    due: sumDue([sum.due, due]),
    overdue: sumDue([sum.overdue, overdue]),
    grades: grades.map((g, i) => sumDue([sum.grades[i], g])),
    statuses: STATUSES.map((s, i) => sum.statuses[i] + statusCount(claims, s)),
  };
  if (rest.length > 0) return <Accumulate key={rest[0]} sheetIds={rest} sum={next} />;
  return (
    <p data-testid="consolidated">
      Consolidated: people <strong data-testid="consolidated-people-count">{next.people}</strong>
      {' '}· Due <strong data-testid="consolidated-due-total">{next.due}</strong>
      {' '}· Overdue <strong data-testid="consolidated-overdue-total">{next.overdue}</strong>
      {GRADES.map((g, i) => (
        <span key={g}>
          {' '}· {g} due <strong data-testid={`consolidated-grade-${g}-due-total`}>{next.grades[i]}</strong>
        </span>
      ))}
      {STATUSES.map((s, i) => (
        <span key={s}>
          {' '}· {s} <strong data-testid={`consolidated-status-${s}-count`}>{next.statuses[i]}</strong>
        </span>
      ))}
    </p>
  );
}

/**
 * The sheet's figures added up with those of all its descendants: people, due-total and overdue-total; per grade, the
 * due-totals of the people on it (retired people left out); per status, the count of claims in it. A sheet with no
 * children shows its own. While the screen is shown as of a date, so is every sheet of the tree: as it stood at the end
 * of that day, with that date as its today (the tree itself is the one named now).
 */
function Consolidated({ sheetId }: { sheetId: string }) {
  const descendants = useQuery(descendantsQuery(sheetId)).data ?? [];
  const sheetIds = [sheetId, ...descendants];
  return <Accumulate key={sheetIds.join('\n')} sheetIds={sheetIds} sum={{ people: 0, due: '0.00', overdue: '0.00', grades: GRADES.map(() => '0.00'), statuses: STATUSES.map(() => 0) }} />;
}

export const contribution = SheetPanel.fill({ order: -9, Component: Consolidated });

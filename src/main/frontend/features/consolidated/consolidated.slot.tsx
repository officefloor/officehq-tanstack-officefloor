import { useCallback, useEffect, useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES, isRetired, usePeople, type Grade } from '../people/people';
import { twoDecimals, useRates } from '../rates/rates';
import { CLAIM_STATUSES, personDueCents, statusCount, useClaims, useLate, useSheetTotals, type ClaimStatus } from '../claims/claims';
import { useSheetToday } from '../sheet/sheet';
import { useCapCents, useFloorCents } from '../settings/settings';
import { useFamily } from './consolidated';
import { Fig } from '../locale/Fig';

type Figures = {
  people: number;
  due: bigint;
  overdue: bigint;
  grades: Record<Grade, bigint>;
  statuses: Record<ClaimStatus, number>;
};

// One sheet's figures as that sheet shows them in its header and summary: its people (retired ones included), its
// due-total and its overdue-total, per grade the due-totals of the people on it (retired ones left out) and per status
// how many claims have it, each figured from the same helpers. Renders nothing; it reports them up.
function SheetFigures({ sheet, report }: { sheet: string; report: (sheet: string, f: Figures) => void }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const cap = useCapCents(sheet);
  const { due, overdue } = useSheetTotals(sheet);
  const count = people.length;
  const cents = overdue.cents;
  const current = people.filter((p) => !isRetired(p));
  const grades = Object.fromEntries(
    GRADES.map((g) => [
      g,
      current
        .filter((p) => p.grade === g)
        .reduce((sum, p) => sum + (personDueCents(claims, rates, p, today, floor, late, cap) ?? 0n), 0n),
    ]),
  ) as Record<Grade, bigint>;
  const statuses = Object.fromEntries(CLAIM_STATUSES.map((s) => [s, statusCount(claims, s)])) as Record<ClaimStatus, number>;
  // Keyed by value so the report runs only when a figure changes.
  const key = JSON.stringify([GRADES.map((g) => grades[g].toString()), CLAIM_STATUSES.map((s) => statuses[s])]);
  useEffect(
    () => report(sheet, { people: count, due, overdue: cents, grades, statuses }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [sheet, report, count, due, cents, key],
  );
  return null;
}

// The consolidated figures over the sheet and all its descendants (see useFamily): the people, the due-total, the
// overdue-total, the per-grade due-totals and the per-status claim counts of each, added up. A sheet with no children shows its own. While the screen shows the sheet as of a
// date, every sheet of the tree is read as it stood at the end of that date, with the date for its today.
function Consolidated({ sheet }: { sheet: string; user: string }) {
  const { data: family = [sheet] } = useFamily(sheet);
  const [figures, setFigures] = useState<Map<string, Figures>>(new Map());
  const report = useCallback(
    (s: string, f: Figures) => setFigures((prev) => new Map(prev).set(s, f)),
    [],
  );
  const shown = family.map((s) => figures.get(s)).filter((f): f is Figures => f !== undefined);
  const people = shown.reduce((n, f) => n + f.people, 0);
  const due = shown.reduce((n, f) => n + f.due, 0n);
  const overdue = shown.reduce((n, f) => n + f.overdue, 0n);
  const gradeDue = (g: Grade) => shown.reduce((n, f) => n + f.grades[g], 0n);
  const statusClaims = (s: ClaimStatus) => shown.reduce((n, f) => n + f.statuses[s], 0);
  return (
    <p>
      {family.map((s) => (
        <SheetFigures key={s} sheet={s} report={report} />
      ))}
      Consolidated: people <strong data-testid="consolidated-people-count">{people}</strong>, due total{' '}
      <strong data-testid="consolidated-due-total"><Fig>{twoDecimals(due)}</Fig></strong>, overdue total{' '}
      <strong data-testid="consolidated-overdue-total"><Fig>{twoDecimals(overdue)}</Fig></strong>
      {GRADES.map((g) => (
        <span key={g}>
          , grade {g} due total{' '}
          <strong data-testid={`consolidated-grade-${g}-due-total`}><Fig>{twoDecimals(gradeDue(g))}</Fig></strong>
        </span>
      ))}
      {CLAIM_STATUSES.map((s) => (
        <span key={s}>
          , {s} claims <strong data-testid={`consolidated-status-${s}-count`}>{statusClaims(s)}</strong>
        </span>
      ))}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: -9, Component: Consolidated });

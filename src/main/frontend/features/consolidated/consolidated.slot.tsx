import { useCallback, useEffect, useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { STATUSES, useClaims } from '../claims/useClaims';
import { useSheetTotals } from '../claims/useSheetTotals';
import { GRADES, usePeople } from '../people/usePeople';
import { hundredths, totalDue, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheet } from '../sheet/useSheet';
import { useGradeDues } from '../summary/useGradeDues';
import { useDescendants } from './useDescendants';

type Figures = {
  people: number;
  due: string;
  overdue: string;
  /** Per grade, the due-totals of the people not retired on it, added up. */
  grades: Record<string, string>;
  /** Per status, how many of the sheet's claims have it. */
  statuses: Record<string, number>;
};

const same = (a: Figures | null, b: Figures | null) =>
  a === b || (a !== null && b !== null && JSON.stringify(a) === JSON.stringify(b));

/** One sheet's figures as that sheet shows them (its header's people count, due-total and
 *  overdue-total), reported once everything they are worked out from has loaded. Renders nothing. */
function SheetFigures({ sheet, report }: { sheet: string; report: (sheet: string, figures: Figures | null) => void }) {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet);
  const today = useSheet(sheet);
  const { dueTotal, overdueTotal } = useSheetTotals(sheet);
  const byGrade = useGradeDues(sheet);
  const ready = people.isSuccess && claims.isSuccess && rates.isSuccess && settings.isSuccess && today.isSuccess;
  const count = people.data?.length ?? 0;
  const grades = JSON.stringify(Object.fromEntries(GRADES.map((g) => [g, totalDue(byGrade[g].map((d) => d.due))])));
  const all = claims.data ?? [];
  const statuses = JSON.stringify(Object.fromEntries(STATUSES.map((s) => [s, all.filter((c) => c.status === s).length])));
  useEffect(() => {
    report(
      sheet,
      ready ? { people: count, due: dueTotal, overdue: overdueTotal, grades: JSON.parse(grades), statuses: JSON.parse(statuses) } : null,
    );
  }, [sheet, ready, count, dueTotal, overdueTotal, grades, statuses, report]);
  useEffect(() => () => report(sheet, null), [sheet, report]);
  return null;
}

/**
 * The consolidated figures over the sheet and all its descendants (see useDescendants): how many
 * people, and each sheet's due-total and overdue-total as that sheet shows them, added up; per grade,
 * each sheet's due-totals of the people not retired on it (as its summary shows them), added up; and
 * per status, how many claims have it over the tree. While the
 * screen shows the sheet as of a date, every sheet of the tree is read as it stood at the end of that
 * date, with that date as its today. A sheet
 * with no children shows its own. Blank until every sheet's figures have loaded.
 */
function Consolidated({ sheet }: { user: string; sheet: string }) {
  const { data: descendants } = useDescendants(sheet);
  const sheets = descendants === undefined ? [] : [sheet, ...descendants];
  const [figures, setFigures] = useState<Record<string, Figures | null>>({});
  const report = useCallback((id: string, f: Figures | null) => {
    setFigures((was) => (same(was[id] ?? null, f) ? was : { ...was, [id]: f }));
  }, []);
  const all = sheets.map((id) => figures[id] ?? null);
  const ready = sheets.length > 0 && all.every((f) => f !== null);
  const loaded = all.filter((f): f is Figures => f !== null);
  return (
    <section data-testid="consolidated">
      {sheets.map((id) => (
        <SheetFigures key={id} sheet={id} report={report} />
      ))}
      <p>
        Consolidated: people <strong data-testid="consolidated-people-count">{ready ? loaded.reduce((n, f) => n + f.people, 0) : ''}</strong>
        , due <strong data-testid="consolidated-due-total">{ready ? totalDue(loaded.map((f) => hundredths(f.due))) : ''}</strong>
        , overdue <strong data-testid="consolidated-overdue-total">{ready ? totalDue(loaded.map((f) => hundredths(f.overdue))) : ''}</strong>
      </p>
      <p>
        Due by grade:
        {GRADES.map((g) => (
          <span key={g}>
            {' '}
            {g} <strong data-testid={`consolidated-grade-${g}-due-total`}>{ready ? totalDue(loaded.map((f) => hundredths(f.grades[g]))) : ''}</strong>
          </span>
        ))}
      </p>
      <p>
        Claims by status:
        {STATUSES.map((s) => (
          <span key={s}>
            {' '}
            {s} <strong data-testid={`consolidated-status-${s}-count`}>{ready ? loaded.reduce((n, f) => n + f.statuses[s], 0) : ''}</strong>
          </span>
        ))}
      </p>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 21, Component: Consolidated });

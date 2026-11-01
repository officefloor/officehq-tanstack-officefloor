import { useCallback, useEffect, useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useClaims } from '../claims/useClaims';
import { useSheetTotals } from '../claims/useSheetTotals';
import { usePeople } from '../people/usePeople';
import { hundredths, totalDue, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheet } from '../sheet/useSheet';
import { useDescendants } from './useDescendants';

type Figures = { people: number; due: string; overdue: string };

const same = (a: Figures | null, b: Figures | null) =>
  a === b || (a !== null && b !== null && a.people === b.people && a.due === b.due && a.overdue === b.overdue);

/** One sheet's figures as that sheet shows them (its header's people count, due-total and
 *  overdue-total), reported once everything they are worked out from has loaded. Renders nothing. */
function SheetFigures({ sheet, report }: { sheet: string; report: (sheet: string, figures: Figures | null) => void }) {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet);
  const today = useSheet(sheet);
  const { dueTotal, overdueTotal } = useSheetTotals(sheet);
  const ready = people.isSuccess && claims.isSuccess && rates.isSuccess && settings.isSuccess && today.isSuccess;
  const count = people.data?.length ?? 0;
  useEffect(() => {
    report(sheet, ready ? { people: count, due: dueTotal, overdue: overdueTotal } : null);
  }, [sheet, ready, count, dueTotal, overdueTotal, report]);
  useEffect(() => () => report(sheet, null), [sheet, report]);
  return null;
}

/**
 * The consolidated figures over the sheet and all its descendants (see useDescendants): how many
 * people, and each sheet's due-total and overdue-total as that sheet shows them, added up. While the
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
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 21, Component: Consolidated });

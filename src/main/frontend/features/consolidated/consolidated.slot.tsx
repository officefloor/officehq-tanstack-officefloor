import { useCallback, useEffect, useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { twoDecimals } from '../rates/rates';
import { useSheetTotals } from '../claims/claims';
import { useFamily } from './consolidated';
import { LiveFigures } from '../../query/live';
import { Fig } from '../locale/Fig';

type Figures = { people: number; due: bigint; overdue: bigint };

// One sheet's figures as that sheet shows them in its header: its people (retired ones included), its due-total and
// its overdue-total, each figured from the same helpers. Renders nothing; it reports them up.
function SheetFigures({ sheet, report }: { sheet: string; report: (sheet: string, f: Figures) => void }) {
  const { data: people = [] } = usePeople(sheet);
  const { due, overdue } = useSheetTotals(sheet);
  const count = people.length;
  const cents = overdue.cents;
  useEffect(() => report(sheet, { people: count, due, overdue: cents }), [sheet, report, count, due, cents]);
  return null;
}

// The consolidated figures over the sheet and all its descendants (see useFamily): the people, the due-total and the
// overdue-total of each, added up. A sheet with no children shows its own. They are the figures as the sheets stand
// now, even while the screen shows the sheet as of a date.
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
  return (
    <p>
      <LiveFigures.Provider value={true}>
        {family.map((s) => (
          <SheetFigures key={s} sheet={s} report={report} />
        ))}
      </LiveFigures.Provider>
      Consolidated: people <strong data-testid="consolidated-people-count">{people}</strong>, due total{' '}
      <strong data-testid="consolidated-due-total"><Fig>{twoDecimals(due)}</Fig></strong>, overdue total{' '}
      <strong data-testid="consolidated-overdue-total"><Fig>{twoDecimals(overdue)}</Fig></strong>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: -9, Component: Consolidated });

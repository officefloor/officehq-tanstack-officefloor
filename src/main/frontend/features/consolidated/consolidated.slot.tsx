import { useMemo } from 'react';
import { QueryClientProvider, useQuery } from '@tanstack/react-query';
import { useAsOf } from '../../api/asOf';
import { nowQueryClient } from '../../query/nowQueryClient';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { useSheetDueTotal } from '../rates/due.slot';
import { useSheetOverdue } from '../rates/overdue.slot';
import { sumDue } from '../rates/ratesQuery';
import { descendantsQuery } from './consolidatedQuery';

type Figures = { people: number; due: string; overdue: string };

/**
 * Adds the figures of the first sheet in `sheetIds` — as that sheet shows them in its header — to `sum`, then hands on
 * to the rest; the last shows the consolidated figures. One component per sheet keeps each sheet's hooks fixed.
 */
function Accumulate({ sheetIds, sum }: { sheetIds: string[]; sum: Figures }) {
  const [sheetId, ...rest] = sheetIds;
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const due = useSheetDueTotal(sheetId);
  const { total: overdue } = useSheetOverdue(sheetId);
  const next = { people: sum.people + people.length, due: sumDue([sum.due, due]), overdue: sumDue([sum.overdue, overdue]) };
  if (rest.length > 0) return <Accumulate key={rest[0]} sheetIds={rest} sum={next} />;
  return (
    <p data-testid="consolidated">
      Consolidated: people <strong data-testid="consolidated-people-count">{next.people}</strong>
      {' '}· Due <strong data-testid="consolidated-due-total">{next.due}</strong>
      {' '}· Overdue <strong data-testid="consolidated-overdue-total">{next.overdue}</strong>
    </p>
  );
}

/** The sheet's figures added up with those of all its descendants: people, due-total and overdue-total. A sheet with no children shows its own. */
function ConsolidatedFigures({ sheetId }: { sheetId: string }) {
  const descendants = useQuery(descendantsQuery(sheetId)).data ?? [];
  const sheetIds = [sheetId, ...descendants];
  return <Accumulate key={sheetIds.join('\n')} sheetIds={sheetIds} sum={{ people: 0, due: '0.00', overdue: '0.00' }} />;
}

/**
 * The consolidated figures are always those of the sheets as they are now: while the screen is shown as of a date they
 * are read by a query client of their own that ignores it (a fresh one for each date, so nothing read before is shown).
 */
function Consolidated({ sheetId }: { sheetId: string }) {
  const asOf = useAsOf();
  const now = useMemo(() => (asOf ? nowQueryClient() : null), [asOf, sheetId]);
  if (!now) return <ConsolidatedFigures sheetId={sheetId} />;
  return (
    <QueryClientProvider client={now}>
      <ConsolidatedFigures sheetId={sheetId} />
    </QueryClientProvider>
  );
}

export const contribution = SheetPanel.fill({ order: -9, Component: Consolidated });

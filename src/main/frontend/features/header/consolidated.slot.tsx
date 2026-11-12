import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { AppHeader } from '../../slots/defs/appHeader';
import { useAsOf, useCurrentSheet } from '../../session/session';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { usePeople } from '../people/people';
import { dueTotal } from '../rates/rates';
import { useSheetDueTotal } from '../rates/due.slot';
import { useSheetOverdue } from '../overdue/overdue.slot';
import { useFigure } from '../locale/locale';

/** Under the sheet's key, so loading the sheet again re-reads its descendants. */
const descendantsKey = (sheet: string) => [...sheetKey(sheet), 'descendants'] as const;

/** The ids of the sheets naming the sheet as their parent, their children, and so on (as of the screen's
 *  as-of date, when one is set). */
function useDescendants(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(descendantsKey(sheet), asOf),
    queryFn: () => getJson<string[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/descendants`, asOf)),
    enabled: sheet !== '',
  });
}

type Figures = { people: number; due: string[]; overdue: string[] };

/** Adds the first sheet's figures (as that sheet shows them) to those so far, then the rest's, one sheet per
 *  component so each reads its sheet with the same hooks; shows the sums once every sheet is added. */
function Consolidate({ sheets, sofar }: { sheets: string[]; sofar: Figures }) {
  const fig = useFigure();
  const [first, ...rest] = sheets;
  const people = usePeople(first);
  const due = useSheetDueTotal(first);
  const overdue = useSheetOverdue(first);
  const figures = {
    people: sofar.people + (people.data ?? []).length,
    due: [...sofar.due, due],
    overdue: [...sofar.overdue, overdue.total],
  };
  if (rest.length > 0) return <Consolidate sheets={rest} sofar={figures} />;
  return (
    <>
      People <strong data-testid="consolidated-people-count">{figures.people}</strong>{' '}
      Due total <strong data-testid="consolidated-due-total">{fig(dueTotal(figures.due))}</strong>{' '}
      Overdue total <strong data-testid="consolidated-overdue-total">{fig(dueTotal(figures.overdue))}</strong>
    </>
  );
}

// The consolidated figures over the sheet and all its descendants: their people, and each sheet's due- and
// overdue-totals (the header's) added up. While the screen has an as-of date, every sheet of the tree is read
// as it stood at the end of that day, with that date as its today. A sheet with no children shows its own.
function ConsolidatedFigures() {
  const [sheet] = useCurrentSheet();
  const descendants = useDescendants(sheet);
  if (sheet === '' || !descendants.data) return null;
  return (
    <p data-testid="consolidated-figures">
      Consolidated{' '}
      <Consolidate key={descendants.data.join(',')} sheets={[sheet, ...descendants.data]} sofar={{ people: 0, due: [], overdue: [] }} />
    </p>
  );
}

export const contribution = AppHeader.fill({ order: 31, Component: ConsolidatedFigures });

import { useCallback, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { ReadNow } from '../../query/readNow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSheetDueTotal, useSheetOverdue } from '../header/totals';
import { usePeople } from '../people/people';
import { sum } from '../rates/rates';

/** The ids of the sheet's descendants: the sheets naming it as their parent, then theirs, and so on. */
function useDescendants(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants'],
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`),
  });
}

/** One sheet's own figures, as its header shows them. */
type Figures = { people: number; due: string; overdue: string };

/** Works out one sheet's figures with the same hooks as its header, and reports them. Shows nothing. */
function SheetFigures({ sheetId, report }: { sheetId: string; report: (sheetId: string, f: Figures) => void }) {
  const people = (usePeople(sheetId).data ?? []).length;
  const due = useSheetDueTotal(sheetId);
  const overdue = useSheetOverdue(sheetId).total;
  useEffect(() => report(sheetId, { people, due, overdue }), [report, sheetId, people, due, overdue]);
  return null;
}

/** The consolidated figures over the sheet and all its descendants: their people counted, and each sheet's
 *  due-total and overdue-total (as that sheet shows it) added up. A sheet with no children shows its own. They are
 *  always the figures as the sheets are now: an as-of date (see features/asof) leaves them as they are. */
function Consolidated({ sheetId }: { sheetId: string }) {
  const descendants = useDescendants(sheetId);
  const sheets = [sheetId, ...(descendants.data ?? [])];
  const [figures, setFigures] = useState<Record<string, Figures>>({});
  const report = useCallback(
    (id: string, f: Figures) => setFigures((all) => ({ ...all, [id]: f })),
    [],
  );
  const counted = sheets.map((id) => figures[id]).filter((f): f is Figures => f !== undefined);

  return (
    <section data-testid="consolidated">
      <ReadNow.Provider value={true}>
        {sheets.map((id) => (
          <SheetFigures key={id} sheetId={id} report={report} />
        ))}
      </ReadNow.Provider>
      Consolidated: people <span data-testid="consolidated-people-count">{counted.reduce((n, f) => n + f.people, 0)}</span>
      {' · '}Due total <span data-testid="consolidated-due-total">{sum(counted.map((f) => f.due))}</span>
      {' · '}Overdue total <span data-testid="consolidated-overdue-total">{sum(counted.map((f) => f.overdue))}</span>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: -9, Component: Consolidated });

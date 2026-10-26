import { useEffect, useState } from 'react';
import { QueryClientProvider, useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { liveQueryClient } from '../../query/queryClient';
import { SheetBody } from '../../slots/defs/sheetBody';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useClaims } from '../claims/useClaims';
import { useOverdue } from '../overdue/overdue.slot';
import { usePeople } from '../people/people.slot';
import { useSheetDueTotal } from '../rates/rates.slot';
import { showHundredths } from '../rates/useRates';

type Figures = { people: number; due: bigint; overdue: bigint };

/** The ids of the sheet's descendants: its children (the sheets naming it as their parent), theirs, and so on down. */
export const descendantsKey = (sheetId: string) => ['sheet', sheetId, 'descendants'] as const;

function useDescendants(sheetId: string) {
  return useQuery({
    queryKey: descendantsKey(sheetId),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`),
  });
}

// One sheet's figures as its own header shows them, reported up once all of them are read.
function SheetFigures({ sheetId, report }: { sheetId: string; report: (id: string, f: Figures | null) => void }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const due = useSheetDueTotal(sheetId);
  const overdue = useOverdue(sheetId).sheet.cents;
  const ready = people.data !== undefined && claims.data !== undefined;
  const count = (people.data ?? []).length;
  useEffect(() => {
    report(sheetId, ready ? { people: count, due, overdue } : null);
  }, [sheetId, report, ready, count, due, overdue]);
  useEffect(() => () => report(sheetId, null), [sheetId, report]);
  return null;
}

// The consolidated figures over the sheet and all its descendants: the people on each, and each sheet's
// due-total and overdue-total as that sheet shows them, added up. A sheet with no children shows its own. They are
// always the figures as the sheets stand now: while the screen shows the sheet as of a date, they are read live.
function Consolidated({ sheetId }: { sheetId: string; user: string }) {
  const [asOf] = useSearchParam('asof', asString);
  const descendants = useDescendants(sheetId);
  const [figures, setFigures] = useState<Map<string, Figures>>(new Map());
  const [report] = useState(() => (id: string, f: Figures | null) =>
    setFigures((m) => {
      const next = new Map(m);
      if (f === null) next.delete(id);
      else next.set(id, f);
      return next;
    }),
  );
  const sheets = descendants.data === undefined ? [] : [sheetId, ...descendants.data];
  const all = sheets.length > 0 && sheets.every((s) => figures.has(s));
  const shown = sheets.map((s) => figures.get(s)).filter((f): f is Figures => f !== undefined);
  const people = shown.reduce((n, f) => n + f.people, 0);
  const due = shown.reduce((n, f) => n + f.due, 0n);
  const overdue = shown.reduce((n, f) => n + f.overdue, 0n);
  return (
    <section data-testid="consolidated">
      {asOf === '' ? (
        sheets.map((s) => <SheetFigures key={s} sheetId={s} report={report} />)
      ) : (
        <QueryClientProvider client={liveQueryClient}>
          {sheets.map((s) => (
            <SheetFigures key={s} sheetId={s} report={report} />
          ))}
        </QueryClientProvider>
      )}
      <p>
        Consolidated people: <strong data-testid="consolidated-people-count">{all ? people : ''}</strong>{' '}
        Consolidated due total: <strong data-testid="consolidated-due-total">{all ? showHundredths(due) : ''}</strong>{' '}
        Consolidated overdue total:{' '}
        <strong data-testid="consolidated-overdue-total">{all ? showHundredths(overdue) : ''}</strong>
      </p>
      {descendants.isError && (
        <p data-testid="consolidated-error" role="alert">
          {descendants.error.message}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -9, Component: Consolidated });

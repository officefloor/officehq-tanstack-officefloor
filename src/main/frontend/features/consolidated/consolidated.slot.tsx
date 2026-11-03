import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useOverdue } from '../overdue/useOverdue';
import { peopleKey, type Person } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { hundredths, money, trimmedSum } from '../rates/useRates';
import { useFigure } from '../locale/useLocale';

/** One sheet's figures, as its header shows them: its people (retired included), and its due-total and
 *  overdue-total. While the screen shows a date, each sheet is read as it stood at the end of that day, with that
 *  date as its today. */
type Figures = { people: number; due: string; overdue: string };

/** The sheet's descendants (the sheets naming it as their parent, theirs, and so on), re-read from the store.
 *  Their own cached reads are dropped first, so each is re-read from the store too. */
function useDescendants(sheetId: string) {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants'],
    queryFn: async () => {
      const { sheets } = await getJson<{ sheets: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`);
      for (const id of sheets) queryClient.removeQueries({ queryKey: ['sheet', id] });
      return sheets;
    },
  });
}

/** The number of the sheet's people (retired included), as of the screen's as-of date when one is set. */
function usePeopleCount(sheetId: string) {
  return useQuery({
    queryKey: [...peopleKey(sheetId), 'count'],
    queryFn: async () =>
      (await getJson<{ people: Person[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/people`)).people.length,
  });
}

/** Works out one sheet's figures with the same hooks its header uses, and hands them up; shows nothing. */
function SheetFigures({ sheetId, onFigures }: { sheetId: string; onFigures: (id: string, f: Figures) => void }) {
  const people = usePeopleCount(sheetId).data ?? 0;
  const due = trimmedSum([...useDueTotals(sheetId).values()]);
  const overdue = trimmedSum([...useOverdue(sheetId).values()].map((f) => f.total));
  useEffect(() => onFigures(sheetId, { people, due, overdue }), [sheetId, people, due, overdue, onFigures]);
  return null;
}

/** The consolidated figures over the sheet and all its descendants: the number of people, and each sheet's
 *  due-total and overdue-total (as that sheet shows them) added up, all as of the screen's as-of date when one is
 *  set. A sheet with no children shows its own. */
function Consolidated({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const descendants = useDescendants(sheetId);
  const [figures, setFigures] = useState<Record<string, Figures>>({});
  const [onFigures] = useState(() => (id: string, f: Figures) => setFigures((all) => ({ ...all, [id]: f })));
  const sheets = [sheetId, ...(descendants.data ?? [])];
  const shown = sheets.map((id) => figures[id]).filter((f): f is Figures => f !== undefined);
  const sum = (pick: (f: Figures) => string) => money(shown.reduce((s, f) => s + hundredths(pick(f)), 0n));
  return (
    <p data-testid="consolidated">
      {sheets.map((id) => (
        <SheetFigures key={id} sheetId={id} onFigures={onFigures} />
      ))}
      Consolidated: people <strong data-testid="consolidated-people-count">{shown.reduce((n, f) => n + f.people, 0)}</strong>
      {' '}· due total <strong data-testid="consolidated-due-total">{fig(sum((f) => f.due))}</strong>
      {' '}· overdue total <strong data-testid="consolidated-overdue-total">{fig(sum((f) => f.overdue))}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: -9, Component: ({ sheetId }) => <Consolidated sheetId={sheetId} /> });

import { useCallback, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { STATUSES, useGradeDue, useSheetDueTotal, useSheetOverdue, useStatusCounts } from '../header/totals';
import { GRADES, usePeople } from '../people/people';
import { sum } from '../rates/rates';

/** The ids of the sheet's descendants: the sheets naming it as their parent, then theirs, and so on. */
function useDescendants(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants'],
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`),
  });
}

/** One sheet's own figures, as its header and summary show them: the due-total per grade and the claim count per
 *  status in the order of GRADES and STATUSES. */
type Figures = { people: number; due: string; overdue: string; grades: string[]; statuses: number[] };

/** Works out one sheet's figures with the same hooks as its header, and reports them. Shows nothing. */
function SheetFigures({ sheetId, report }: { sheetId: string; report: (sheetId: string, f: Figures) => void }) {
  const people = (usePeople(sheetId).data ?? []).length;
  const due = useSheetDueTotal(sheetId);
  const overdue = useSheetOverdue(sheetId).total;
  const gradeDue = useGradeDue(sheetId);
  const statusCount = useStatusCounts(sheetId);
  // Joined into strings so the effect re-runs only when a value changes, not on every new array.
  const grades = GRADES.map(gradeDue).join(',');
  const statuses = STATUSES.map(statusCount).join(',');
  useEffect(
    () =>
      report(sheetId, {
        people,
        due,
        overdue,
        grades: grades.split(','),
        statuses: statuses.split(',').map(Number),
      }),
    [report, sheetId, people, due, overdue, grades, statuses],
  );
  return null;
}

/** The consolidated figures over the sheet and all its descendants: their people counted, and each sheet's
 *  due-total and overdue-total (as that sheet shows it) added up, and per grade and per status each sheet's grade
 *  due-total and status count (as its summary shows them) added up. A sheet with no children shows its own. While an
 *  as-of date is set (see features/asof), every sheet of the tree is read as it stood at the end of that date, with
 *  that date as its today. */
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
      {sheets.map((id) => (
        <SheetFigures key={id} sheetId={id} report={report} />
      ))}
      Consolidated: people <span data-testid="consolidated-people-count">{counted.reduce((n, f) => n + f.people, 0)}</span>
      {' · '}Due total <span data-testid="consolidated-due-total">{sum(counted.map((f) => f.due))}</span>
      {' · '}Overdue total <span data-testid="consolidated-overdue-total">{sum(counted.map((f) => f.overdue))}</span>
      <table data-testid="consolidated-grades">
        <tbody>
          {GRADES.map((g, i) => (
            <tr key={g}>
              <td>Grade {g} due total</td>
              <td data-testid={`consolidated-grade-${g}-due-total`}>{sum(counted.map((f) => f.grades[i]))}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="consolidated-statuses">
        <tbody>
          {STATUSES.map((s, i) => (
            <tr key={s}>
              <td>{s} claims</td>
              <td data-testid={`consolidated-status-${s}-count`}>{counted.reduce((n, f) => n + f.statuses[i], 0)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: -9, Component: Consolidated });

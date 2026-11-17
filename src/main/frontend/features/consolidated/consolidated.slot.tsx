import { useCallback, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { STATUSES, useGradeDue, usePeopleDue, usePeopleOverdue, useStatusCounts } from '../header/totals';
import { GRADES, usePeople } from '../people/people';
import { sum, trimmedSum } from '../rates/rates';

/** The ids of the sheet's descendants: the sheets naming it as their parent, then theirs, and so on. */
function useDescendants(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants'],
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`),
  });
}

/** One sheet's own figures: its people's names (each person's name then aliases), due-totals and overdue-totals (as
 *  their rows show them, in the same order), and as its summary shows them the due-total per grade and the claim count
 *  per status in the order of GRADES and STATUSES. */
type Figures = { names: string[][]; due: string[]; overdue: string[]; grades: string[]; statuses: number[] };

/** How many people the sheets' people make, each counted once over them all: people whose names or aliases match (as
 *  names are compared: case and outer spaces ignored) are one person. */
function distinctPeople(names: string[][]): number {
  const parent = names.map((_, i) => i);
  const root = (i: number): number => (parent[i] === i ? i : (parent[i] = root(parent[i])));
  const byKey = new Map<string, number>();
  names.forEach((ns, i) => {
    for (const n of ns) {
      const key = n.trim().toLowerCase();
      const other = byKey.get(key);
      if (other === undefined) byKey.set(key, i);
      else parent[root(i)] = root(other);
    }
  });
  return names.filter((_, i) => root(i) === i).length;
}

/** Works out one sheet's figures with the same hooks as its header, and reports them. Shows nothing. */
function SheetFigures({ sheetId, report }: { sheetId: string; report: (sheetId: string, f: Figures) => void }) {
  const people = usePeople(sheetId).data ?? [];
  const peopleDue = usePeopleDue(sheetId);
  const peopleOverdue = usePeopleOverdue(sheetId);
  // Joined into strings so the effect re-runs only when a value changes, not on every new array.
  const names = JSON.stringify(people.map((p) => [p.name, ...(p.aliases ?? [])]));
  const due = people.map((p) => peopleDue.get(p.id) ?? 'n/a').join(',');
  const overdue = people.map((p) => peopleOverdue.get(p.id) ?? '0.00').join(',');
  const gradeDue = useGradeDue(sheetId);
  const statusCount = useStatusCounts(sheetId);
  const grades = GRADES.map(gradeDue).join(',');
  const statuses = STATUSES.map(statusCount).join(',');
  useEffect(
    () =>
      report(sheetId, {
        names: JSON.parse(names) as string[][],
        due: due === '' ? [] : due.split(','),
        overdue: overdue === '' ? [] : overdue.split(','),
        grades: grades.split(','),
        statuses: statuses.split(',').map(Number),
      }),
    [report, sheetId, names, due, overdue, grades, statuses],
  );
  return null;
}

/** The consolidated figures over the sheet and all its descendants: their people counted once over the tree (see
 *  distinctPeople), the due-total and overdue-total trimmed once over the whole tree (every person's figure added up,
 *  leaving out the single highest and lowest of all people of the tree when three or more have one), and per grade and per status each sheet's grade
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
      Consolidated: people <span data-testid="consolidated-people-count">{distinctPeople(counted.flatMap((f) => f.names))}</span>
      {' · '}Due total <span data-testid="consolidated-due-total">{trimmedSum(counted.flatMap((f) => f.due))}</span>
      {' · '}Overdue total <span data-testid="consolidated-overdue-total">{trimmedSum(counted.flatMap((f) => f.overdue))}</span>
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

import { useEffect, useState } from 'react';
import { useIsFetching, useQuery } from '@tanstack/react-query';
import { listPeople, peopleKey } from '../people/api';
import { useDueTotal } from '../rates/DueTotal';
import { useOverdue } from '../rates/Overdue';
import { formatCents, hundredths } from '../rates/api';
import { useAsOf } from '../sheet/AsOf';
import { descendantsKey, listDescendants, liveKey } from '../sheet/api';
import { Fig } from '../session/Fig';
import { GRADES } from '../people/api';
import { claimsKey, listClaims } from '../claims/api';
import { STATUSES, statusCount, useGradeDues } from './Summary';

/** A sheet's own figures, as that sheet shows them in its header. */
type Figures = { people: number; due: string; overdue: string; grades: Record<string, string>; statuses: Record<string, number> };

/** As the screen shows the sheet: as of its date while one is set, each sheet as it stood then. */
function useFigures(sheetId: string, user: string): Figures {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const statuses = Object.fromEntries(STATUSES.map((s) => [s, statusCount(claims.data, s)]));
  return { people: (people.data ?? []).length, due: useDueTotal(sheetId, user), overdue: useOverdue(sheetId, user).total,
    grades: useGradeDues(sheetId, user), statuses };
}

/** Works out one descendant sheet's own figures and hands them up; shows nothing. */
function Descendant({ sheetId, user, report }: { sheetId: string; user: string; report: (id: string, f: Figures) => void }) {
  const f = useFigures(sheetId, user);
  useEffect(() => report(sheetId, f), [sheetId, f.people, f.due, f.overdue, JSON.stringify(f.grades), JSON.stringify(f.statuses)]);
  return null;
}

/** The consolidated figures over the sheet and all its descendants (its children, theirs, ...): the
 *  number of people, and each sheet's due- and overdue-total as that sheet shows it, added up; per grade,
 *  each sheet's due-total of that grade (retired people left out, as its summary shows it) added up; per
 *  status, the number of claims in it over the tree.
 *  While the screen shows the sheet as of a date, every sheet of the tree is as it stood at the end of
 *  that day, with that date as its today; the tree itself is the one the sheets' parents make now. */
export function Consolidated({ sheetId, user }: { sheetId: string; user: string }) {
  const [asOf] = useAsOf();
  // The tree is the one the sheets' parents make now, even while the screen shows a date.
  const live = asOf !== '';
  const own = useFigures(sheetId, user);
  const descendants = useQuery({ queryKey: liveKey(descendantsKey(sheetId), live), queryFn: () => listDescendants(sheetId, live) });
  const ids = descendants.data ?? [];
  const [reported, setReported] = useState<Record<string, Figures>>({});
  const report = (id: string, f: Figures) => setReported((r) => ({ ...r, [id]: f }));
  // A descendant's figures count once its own queries have been read afresh.
  const fetching = useIsFetching({ predicate: (q) => q.queryKey[0] === 'sheet' && ids.includes(q.queryKey[1] as string) });
  const ready = descendants.isSuccess && fetching === 0 && ids.every((id) => reported[id] !== undefined);
  const all = [own, ...ids.map((id) => reported[id]).filter((f): f is Figures => f !== undefined)];
  const sum = (pick: (f: Figures) => string) => formatCents(all.reduce((t, f) => t + hundredths(pick(f)), 0n));
  return (
    <p>
      {ids.map((id) => <Descendant key={id} sheetId={id} user={user} report={report} />)}
      Consolidated: people <span data-testid="consolidated-people-count">{ready ? all.reduce((t, f) => t + f.people, 0) : ''}</span>
      {' · '}due <span data-testid="consolidated-due-total"><Fig>{ready ? sum((f) => f.due) : ''}</Fig></span>
      {' · '}overdue <span data-testid="consolidated-overdue-total"><Fig>{ready ? sum((f) => f.overdue) : ''}</Fig></span>
      {GRADES.map((g) => (
        <span key={g}>{' · '}grade {g} due <span data-testid={`consolidated-grade-${g}-due-total`}>
          <Fig>{ready ? sum((f) => f.grades[g]) : ''}</Fig></span></span>
      ))}
      {STATUSES.map((s) => (
        <span key={s}>{' · '}{s} <span data-testid={`consolidated-status-${s}-count`}>
          {ready ? all.reduce((t, f) => t + f.statuses[s], 0) : ''}</span></span>
      ))}
    </p>
  );
}

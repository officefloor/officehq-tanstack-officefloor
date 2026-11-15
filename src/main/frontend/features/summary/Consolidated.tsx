import { useEffect, useState } from 'react';
import { useIsFetching, useQuery } from '@tanstack/react-query';
import { listPeople, peopleKey } from '../people/api';
import { usePersonDues } from '../rates/DueTotal';
import { useOverdue } from '../rates/Overdue';
import { formatCents, hundredths, trimmedSum } from '../rates/api';
import { useAsOf } from '../sheet/AsOf';
import { descendantsKey, listDescendants, liveKey } from '../sheet/api';
import { Fig } from '../session/Fig';
import { GRADES } from '../people/api';
import { claimsKey, listClaims } from '../claims/api';
import { STATUSES, statusCount, useGradeDues } from './Summary';

/** A sheet's own figures: each person's names (their name and aliases), due-total and overdue-total as
 *  their row shows them, and per grade and status the figures as the sheet's summary shows them. */
type Figures = { names: string[][]; dues: Array<string | null>; overdues: string[]; grades: Record<string, string>;
  statuses: Record<string, number> };

const nameKey = (name: string) => name.trim().toLowerCase();

/** How many people over the tree: people whose names or aliases match (as names are compared) are one. */
function distinctPeople(names: string[][]): number {
  const parent = names.map((_, i) => i);
  const find = (i: number): number => (parent[i] === i ? i : (parent[i] = find(parent[i])));
  const seen = new Map<string, number>();
  names.forEach((ns, i) => ns.forEach((n) => {
    const k = nameKey(n);
    const j = seen.get(k);
    if (j === undefined) seen.set(k, i);
    else parent[find(i)] = find(j);
  }));
  return names.filter((_, i) => find(i) === i).length;
}

/** As the screen shows the sheet: as of its date while one is set, each sheet as it stood then. */
function useFigures(sheetId: string, user: string): Figures {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const statuses = Object.fromEntries(STATUSES.map((s) => [s, statusCount(claims.data, s)]));
  return { names: (people.data ?? []).map((p) => [p.name, ...p.aliases]), dues: usePersonDues(sheetId, user),
    overdues: useOverdue(sheetId, user).perPerson, grades: useGradeDues(sheetId, user), statuses };
}

/** Works out one descendant sheet's own figures and hands them up; shows nothing. */
function Descendant({ sheetId, user, report }: { sheetId: string; user: string; report: (id: string, f: Figures) => void }) {
  const f = useFigures(sheetId, user);
  useEffect(() => report(sheetId, f), [sheetId, JSON.stringify(f)]);
  return null;
}

/** The consolidated figures over the sheet and all its descendants (its children, theirs, ...): the
 *  number of people, each counted once (people whose names or aliases match, as names are compared, are
 *  one); the due- and overdue-total of every person of the tree as their row shows it, added up leaving
 *  out the single highest and lowest over the whole tree when three or more have one; per grade,
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
      Consolidated: people <span data-testid="consolidated-people-count">{ready ? distinctPeople(all.flatMap((f) => f.names)) : ''}</span>
      {' · '}due <span data-testid="consolidated-due-total"><Fig>{ready ? trimmedSum(all.flatMap((f) => f.dues)) : ''}</Fig></span>
      {' · '}overdue <span data-testid="consolidated-overdue-total"><Fig>{ready ? trimmedSum(all.flatMap((f) => f.overdues)) : ''}</Fig></span>
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

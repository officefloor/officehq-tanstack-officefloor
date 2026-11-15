import { useEffect, useState } from 'react';
import { useIsFetching, useQuery } from '@tanstack/react-query';
import { listPeople, peopleKey } from '../people/api';
import { useDueTotal } from '../rates/DueTotal';
import { useOverdue } from '../rates/Overdue';
import { formatCents, hundredths } from '../rates/api';
import { useAsOf } from '../sheet/AsOf';
import { descendantsKey, listDescendants, liveKey } from '../sheet/api';
import { Fig } from '../session/Fig';

/** A sheet's own figures, as that sheet shows them in its header. */
type Figures = { people: number; due: string; overdue: string };

/** `live`: as the sheet is now, even while the screen shows it as of a date. */
function useFigures(sheetId: string, user: string, live: boolean): Figures {
  const people = useQuery({ queryKey: liveKey(peopleKey(sheetId), live), queryFn: () => listPeople(sheetId, live) });
  return { people: (people.data ?? []).length, due: useDueTotal(sheetId, user, live), overdue: useOverdue(sheetId, user, live).total };
}

/** Works out one descendant sheet's own figures and hands them up; shows nothing. */
function Descendant({ sheetId, user, live, report }: { sheetId: string; user: string; live: boolean; report: (id: string, f: Figures) => void }) {
  const f = useFigures(sheetId, user, live);
  useEffect(() => report(sheetId, f), [sheetId, f.people, f.due, f.overdue]);
  return null;
}

/** The consolidated figures over the sheet and all its descendants (its children, theirs, ...): the
 *  number of people, and each sheet's due- and overdue-total as that sheet shows it, added up. They
 *  are the sheets as they are now, also while the screen shows the sheet as of a date. */
export function Consolidated({ sheetId, user }: { sheetId: string; user: string }) {
  const [asOf] = useAsOf();
  const live = asOf !== '';
  const own = useFigures(sheetId, user, live);
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
      {ids.map((id) => <Descendant key={id} sheetId={id} user={user} live={live} report={report} />)}
      Consolidated: people <span data-testid="consolidated-people-count">{ready ? all.reduce((t, f) => t + f.people, 0) : ''}</span>
      {' · '}due <span data-testid="consolidated-due-total"><Fig>{ready ? sum((f) => f.due) : ''}</Fig></span>
      {' · '}overdue <span data-testid="consolidated-overdue-total"><Fig>{ready ? sum((f) => f.overdue) : ''}</Fig></span>
    </p>
  );
}

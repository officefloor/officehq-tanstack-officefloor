import { useEffect, useRef, useState } from 'react';
import { useIsFetching, useQuery, useQueryClient } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { listRates, personDue, rateByGrade, ratesKey, showDue } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { listTree, sheetKey, treeKey, useSheetToday } from '../sheet/api';

/** A person as the search finds them: their name, aliases and due-total as their row shows it. */
type Found = { name: string; aliases: string[]; due: string };

/** Everyone on a sheet in list order, with their due-total as that sheet's list shows it; undefined until read. */
function useSheetPeople(sheetId: string, user: string): Found[] | undefined {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  if (!people.isSuccess || !claims.isSuccess || !rates.isSuccess || today === '') return undefined;
  const byGrade = rateByGrade(rates.data, today);
  return people.data.map((p) => ({
    name: p.name,
    aliases: p.aliases,
    due: showDue(personDue(liveClaims(claims.data.filter((c) => c.personId === p.id), [p]),
      rates.data, (d) => gradeOn(p, d), byGrade.get(p.grade), floor, charges, cap)),
  }));
}

/** Works out one sheet's people and hands them up; shows nothing. */
function TreeSheet({ sheetId, user, report }: { sheetId: string; user: string; report: (id: string, f: Found[]) => void }) {
  const found = useSheetPeople(sheetId, user);
  const seen = JSON.stringify(found ?? null);
  useEffect(() => {
    if (found !== undefined) report(sheetId, found);
  }, [sheetId, seen]);
  return null;
}

/** A search box and a button that list, over the sheet and every other sheet of its tree, everyone
 *  whose name or an alias contains the typed text (case ignored), one line each `<sheet id> <name>
 *  <due-total>`: the sheet's own people first in list order, then the other sheets' by sheet id. The
 *  count is cleared while a search runs; every sheet is read afresh for it. */
export function TreeSearch({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  // The search running or done: the text searched for and the tree's sheets (null: still being read).
  const [run, setRun] = useState<{ n: number; text: string; ids: string[] | null } | null>(null);
  const [failed, setFailed] = useState(false);
  const [reported, setReported] = useState<Record<string, Found[]>>({});
  const runs = useRef(0);

  // Another sheet loaded: the last search was over another tree.
  useEffect(() => {
    runs.current++;
    setRun(null);
    setFailed(false);
  }, [sheetId]);

  const search = async () => {
    const n = ++runs.current;
    const typed = text;
    setRun({ n, text: typed, ids: null });
    setReported({});
    setFailed(false);
    try {
      const tree = await queryClient.fetchQuery({ queryKey: treeKey(sheetId), queryFn: () => listTree(sheetId), staleTime: 0 });
      // Every sheet of the tree is read afresh, also those read before.
      await Promise.all(tree.map((id) => queryClient.invalidateQueries({ queryKey: sheetKey(id), refetchType: 'all' })));
      if (runs.current !== n) return;
      const others = tree.filter((id) => id !== sheetId).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
      setRun({ n, text: typed, ids: [sheetId, ...others] });
    } catch {
      if (runs.current === n) {
        setRun(null);
        setFailed(true);
      }
    }
  };

  const ids = run?.ids ?? [];
  const fetching = useIsFetching({ predicate: (q) => q.queryKey[0] === 'sheet' && ids.includes(q.queryKey[1] as string) });
  const ready = run !== null && run.ids !== null && fetching === 0 && ids.every((id) => reported[id] !== undefined);
  const needle = (run?.text ?? '').toLowerCase();
  const lines = ready
    ? ids.flatMap((id) => reported[id]
      .filter((f) => [f.name, ...f.aliases].some((s) => s.toLowerCase().includes(needle)))
      .map((f) => `${id} ${f.name} ${f.due}`))
    : [];

  return (
    <div>
      {run?.ids?.map((id) => (
        <TreeSheet key={`${run.n}-${id}`} sheetId={id} user={user}
          report={(sid, f) => setReported((r) => ({ ...r, [sid]: f }))} />
      ))}
      <label>
        Search the tree{' '}
        <input data-testid="search-name" value={text} onChange={(e) => {
          setText(e.target.value);
          setFailed(false);
        }} />
      </label>{' '}
      <button type="button" data-testid="search" onClick={() => void search()}>
        Search
      </button>{' '}
      Found <span data-testid="search-count">{ready ? lines.length : ''}</span>
      {failed && <span data-testid="search-error"> The tree could not be searched.</span>}
      <ul>
        {lines.map((l, i) => (
          <li key={i} data-testid="search-line">{l}</li>
        ))}
      </ul>
    </div>
  );
}

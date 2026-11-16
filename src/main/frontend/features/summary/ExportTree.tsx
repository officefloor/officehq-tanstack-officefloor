import { useEffect, useRef, useState } from 'react';
import { useIsFetching, useQueryClient } from '@tanstack/react-query';
import { downloadCsv } from '../people/ExportPeople';
import { listTree, sheetKey, treeKey } from '../sheet/api';
import { TreeSheet, type Found } from './TreeSearch';

/** The "export tree" button: downloads the people of every sheet of the sheet's tree as a CSV file,
 *  one line each `sheet,name,grade,due-total` (due-total as their row shows it): the sheet's own people
 *  first in list order, then the other sheets' by sheet id. Every sheet is read afresh for it. */
export function ExportTree({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  // The export running: the tree's sheets (null: still being read).
  const [run, setRun] = useState<{ n: number; ids: string[] | null } | null>(null);
  const [failed, setFailed] = useState(false);
  const [reported, setReported] = useState<Record<string, Found[]>>({});
  const runs = useRef(0);

  // Another sheet loaded: an export still running was for another tree.
  useEffect(() => {
    runs.current++;
    setRun(null);
    setFailed(false);
  }, [sheetId]);

  const start = async () => {
    const n = ++runs.current;
    setRun({ n, ids: null });
    setReported({});
    setFailed(false);
    try {
      const tree = await queryClient.fetchQuery({ queryKey: treeKey(sheetId), queryFn: () => listTree(sheetId), staleTime: 0 });
      await Promise.all(tree.map((id) => queryClient.invalidateQueries({ queryKey: sheetKey(id), refetchType: 'all' })));
      if (runs.current !== n) return;
      const others = tree.filter((id) => id !== sheetId).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
      setRun({ n, ids: [sheetId, ...others] });
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

  useEffect(() => {
    if (!ready || run === null) return;
    downloadCsv('tree.csv', [['sheet', 'name', 'grade', 'due-total'],
      ...ids.flatMap((id) => reported[id].map((f) => [id, f.name, f.grade, f.due]))]);
    setRun(null);
  }, [ready]);

  return (
    <span>
      {run?.ids?.map((id) => (
        <TreeSheet key={`${run.n}-${id}`} sheetId={id} user={user}
          report={(sid, f) => setReported((r) => ({ ...r, [sid]: f }))} />
      ))}
      <button type="button" data-testid="export-tree" disabled={run !== null} onClick={() => void start()}>
        Export tree
      </button>
      {failed && <span data-testid="export-tree-error"> The tree could not be exported.</span>}
    </span>
  );
}

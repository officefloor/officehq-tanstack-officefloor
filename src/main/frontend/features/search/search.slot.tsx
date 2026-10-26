import { useCallback, useEffect, useState } from 'react';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { liveQueryClient } from '../../query/queryClient';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { usePeople } from '../people/people.slot';
import { NA, useDueTotals } from '../rates/rates.slot';
import { showHundredths } from '../rates/useRates';

/** The ids of the other sheets of the sheet's tree (its ancestors and descendants), sorted. */
const treeKey = (sheetId: string) => ['sheet', sheetId, 'tree'] as const;

function useTree(sheetId: string) {
  return useQuery({
    queryKey: treeKey(sheetId),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/tree`),
  });
}

type Run = { id: number; text: string; client: QueryClient };

// One sheet's matches, in list order (retired people included): `<sheet id> <name> <due-total>` with the due-total as
// the person's row shows it, reported up once read.
function SheetMatches({ sheetId, text, report }: { sheetId: string; text: string;
  report: (id: string, lines: string[] | null) => void }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const totals = useDueTotals(sheetId);
  const ready = people.data !== undefined && claims.data !== undefined;
  const needle = text.toLowerCase();
  const lines = (people.data ?? [])
    .filter((p) => [p.name, ...(p.aliases ?? [])].some((n) => n.toLowerCase().includes(needle)))
    .map((p) => {
      const total = totals.get(p.id);
      return `${sheetId} ${p.name} ${total == null ? NA : showHundredths(total)}`;
    });
  const joined = lines.join('\n');
  useEffect(() => {
    report(sheetId, ready ? (joined === '' ? [] : joined.split('\n')) : null);
  }, [sheetId, report, ready, joined]);
  return null;
}

// One press of the search: the tree read afresh, then every sheet's matches.
function SearchRun({ sheetId, text, report }: { sheetId: string; text: string;
  report: (sheets: string[] | null, id?: string, lines?: string[] | null) => void }) {
  const tree = useTree(sheetId);
  const sheets = tree.data === undefined ? null : [sheetId, ...tree.data];
  const order = sheets?.join('\n') ?? null;
  useEffect(() => {
    if (order !== null) report(order.split('\n'));
  }, [order, report]);
  const reportSheet = useCallback((id: string, lines: string[] | null) => {
    if (order !== null) report(order.split('\n'), id, lines);
  }, [order, report]);
  useEffect(() => {
    if (tree.isError) report(null, '', [tree.error.message]);
  }, [tree.isError, tree.error, report]);
  return (
    <>
      {(sheets ?? []).map((s) => (
        <SheetMatches key={s} sheetId={s} text={text} report={reportSheet} />
      ))}
    </>
  );
}

// The tree search: a box and a button listing, over this sheet and every other sheet of its tree, every person whose
// name or an alias contains the typed text (case ignored), one line per person as `<sheet id> <name> <due-total>`:
// this sheet's people first in list order, then the other sheets' by sheet id. Each press reads everything afresh, as
// the sheets stand now; the count shows once all of it is read.
function TreeSearch({ sheetId }: { sheetId: string; user: string }) {
  const [text, setText] = useState('');
  const [run, setRun] = useState<Run | null>(null);
  const [sheets, setSheets] = useState<string[] | null>(null);
  const [found, setFound] = useState<Map<string, string[]>>(new Map());
  const [error, setError] = useState<string | null>(null);
  const [report] = useState(() => (order: string[] | null, id?: string, lines?: string[] | null) => {
    if (order === null) {
      setError(lines?.[0] ?? 'Search failed');
      return;
    }
    setSheets((s) => (s !== null && s.join('\n') === order.join('\n') ? s : order));
    if (id === undefined) return;
    setFound((m) => {
      const next = new Map(m);
      if (lines == null) next.delete(id);
      else next.set(id, lines);
      return next;
    });
  });

  useEffect(() => {
    setRun(null);
    setSheets(null);
    setFound(new Map());
  }, [sheetId]);

  const search = () => {
    setSheets(null);
    setFound(new Map());
    setError(null);
    setRun((r) => ({ id: (r?.id ?? 0) + 1, text, client: new QueryClient({ defaultOptions: liveQueryClient.getDefaultOptions() }) }));
  };

  const done = sheets !== null && sheets.every((s) => found.has(s));
  const lines = done ? sheets.flatMap((s) => found.get(s) ?? []) : [];
  return (
    <section data-testid="search-panel">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          search();
        }}
      >
        <input
          data-testid="search-name"
          placeholder="Search the tree"
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            setError(null);
          }}
        />{' '}
        <button type="submit" data-testid="search">
          Search
        </button>{' '}
        Found: <strong data-testid="search-count">{done ? lines.length : ''}</strong>
      </form>
      {run !== null && (
        <QueryClientProvider key={run.id} client={run.client}>
          <SearchRun sheetId={sheetId} text={run.text} report={report} />
        </QueryClientProvider>
      )}
      <ul>
        {lines.map((l, i) => (
          <li key={i} data-testid="search-line">
            {l}
          </li>
        ))}
      </ul>
      {error !== null && (
        <p data-testid="search-error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -8, Component: TreeSearch });

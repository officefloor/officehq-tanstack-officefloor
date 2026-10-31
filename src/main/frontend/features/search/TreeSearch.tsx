import { useState } from 'react';
import { useIsFetching, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { LiveReads } from '../asof/useAsOf';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSheetFigures } from '../people/useSheetFigures';
import { showDue } from '../rates/due';
import { shownName } from '../people/usePeople';

export const treeKey = (sheet: string) => ['sheet', sheet, 'tree'] as const;

/** The other sheets of the sheet's tree (its root and every descendant of the root), by sheet id. */
export function useTree(sheet: string): string[] | undefined {
  const { data } = useQuery({
    queryKey: treeKey(sheet),
    queryFn: () => getJson<{ sheets: string[] }>(`/api/sheets/${encodeURIComponent(sheet)}/tree`),
    enabled: sheet !== '',
  });
  return data?.sheets;
}

// Searches the sheet and every other sheet of its tree for the people whose name or an alias
// contains the typed text (case ignored; blank matches everyone), one line per person:
// <sheet id> <name> (<grade in force on that sheet's today>) <due-total as their row shows it>. The sheet's people come first in list order
// (retired too), then the other sheets' by sheet id. Each press re-reads every sheet, live figures
// also while the screen is shown as of a date; the count is gone until the new lines are all in.
export function TreeSearch() {
  const [sheet] = useSearchParam('sheet', asString);
  const [text, setText] = useState('');
  const [run, setRun] = useState<{ n: number; text: string } | null>(null);
  const queryClient = useQueryClient();

  const search = async () => {
    setRun(null);
    // Re-read what is on screen, and drop what is held for anything not, so each sheet is read afresh.
    await queryClient.invalidateQueries({ queryKey: ['sheet'] });
    queryClient.removeQueries({ queryKey: ['sheet'], type: 'inactive' });
    setRun((was) => ({ n: (was?.n ?? 0) + 1, text }));
  };

  return (
    <section data-testid="tree-search">
      <input data-testid="search-name" value={text} placeholder="Name or alias" onChange={(e) => setText(e.target.value)} />{' '}
      <button type="button" data-testid="search" onClick={() => void search()}>
        Search tree
      </button>
      {run !== null && (
        <LiveReads.Provider value={true}>
          <Results key={run.n} sheet={sheet} text={run.text} />
        </LiveReads.Provider>
      )}
    </section>
  );
}

function Results({ sheet, text }: { sheet: string; text: string }) {
  const tree = useTree(sheet);
  if (tree === undefined) return null;
  return <Add sheets={[sheet, ...tree]} text={text.toLowerCase()} lines={[]} />;
}

// Adds the first sheet's lines and passes on to the rest: one sheet's hooks per component, so the
// number of sheets can change.
function Add({ sheets, text, lines }: { sheets: string[]; text: string; lines: string[] }) {
  if (sheets.length === 0) return <Shown lines={lines} />;
  return <AddSheet key={sheets[0]} sheets={sheets} text={text} lines={lines} />;
}

function AddSheet({ sheets, text, lines }: { sheets: string[]; text: string; lines: string[] }) {
  const sheet = sheets[0];
  const { people, totalOf, today } = useSheetFigures(sheet);
  const queryClient = useQueryClient();
  const fetching = useIsFetching({ queryKey: ['sheet', sheet] });
  const loaded = queryClient
    .getQueryCache()
    .findAll({ queryKey: ['sheet', sheet] })
    .every((q) => q.state.status !== 'pending');
  if (fetching > 0 || !loaded) return null;
  const matches = (name: string) => name.toLowerCase().includes(text);
  const found = people
    .filter((p) => matches(p.name) || p.aliases.some(matches))
    .map((p) => `${sheet} ${shownName(p, today)} ${showDue(totalOf(p))}`);
  return <Add sheets={sheets.slice(1)} text={text} lines={[...lines, ...found]} />;
}

function Shown({ lines }: { lines: string[] }) {
  return (
    <div>
      Found: <span data-testid="search-count">{lines.length}</span>
      <ul>
        {lines.map((line, i) => (
          <li key={i} data-testid="search-line">
            {line}
          </li>
        ))}
      </ul>
    </div>
  );
}

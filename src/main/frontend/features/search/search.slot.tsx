import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { LiveView, useCurrentSheet } from '../../session/session';
import { sheetKey, useSheet } from '../sheet/sheet';
import { type Person, usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useSettings } from '../settings/settings';
import { usePersonDue } from '../rates/due.slot';

/** Under the sheet's key, so loading the sheet again re-reads its tree. */
const treeKey = (sheet: string) => [...sheetKey(sheet), 'tree'] as const;

type Search = { text: string; sheets: string[]; n: number };

const matches = (p: Person, text: string) => {
  const t = text.toLowerCase();
  return [p.name, ...p.aliases].some((n) => n.toLowerCase().includes(t));
};

/** Reads the first sheet's matching people (each with their due-total as their row shows it), adds their lines
 *  to those so far, then the rest's, one sheet per component so each reads its sheet with the same hooks;
 *  reports the lines once every sheet is read in full. */
function Gather({ sheets, text, sofar, done }: { sheets: string[]; text: string; sofar: string[]; done: (lines: string[]) => void }) {
  const [first, ...rest] = sheets;
  const people = usePeople(first);
  const claims = useClaims(first);
  const rates = useRates(first);
  const settings = useSettings(first);
  const sheet = useSheet(first);
  const dueOf = usePersonDue(first);
  const ready = !!(people.data && claims.data && rates.data && settings.data && sheet.data);
  const lines = ready
    ? [...sofar, ...people.data!.filter((p) => matches(p, text)).map((p) => `${first} ${p.name} ${dueOf(p)}`)]
    : sofar;
  const last = ready && rest.length === 0;
  const key = lines.join('\n');
  useEffect(() => {
    if (last) done(key === '' ? [] : key.split('\n'));
  }, [last, key]); // eslint-disable-line react-hooks/exhaustive-deps
  if (ready && rest.length > 0) return <Gather sheets={rest} text={text} sofar={lines} done={done} />;
  return null;
}

// A search over the sheet and every other sheet of its tree: every person whose name or an alias contains the
// typed text (case ignored), one line per person as <sheet id> <name> <due-total>, this sheet's people first
// in list order (retired people included), then the other sheets' by sheet id, as they are now. The count is
// gone while a search runs.
function TreeSearch() {
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [search, setSearch] = useState<Search | null>(null);
  const [lines, setLines] = useState<string[] | null>(null);
  const [n, setN] = useState(0);

  useEffect(() => {
    setSearch(null);
    setLines(null);
  }, [sheet]);

  const run = async () => {
    const at = n + 1;
    setN(at);
    setSearch(null);
    setLines(null);
    const others = await queryClient.fetchQuery({
      queryKey: treeKey(sheet),
      queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tree`),
      staleTime: 0,
    });
    const sorted = [...others].sort();
    await Promise.all(sorted.map((s) => queryClient.refetchQueries({ queryKey: sheetKey(s) })));
    setSearch({ text, sheets: [sheet, ...sorted], n: at });
  };

  if (sheet === '') return null;
  return (
    <section>
      <h2>Search the tree</h2>
      <input data-testid="search-name" value={text} onChange={(e) => setText(e.target.value)} />{' '}
      <button type="button" data-testid="search" onClick={() => void run()}>
        Search
      </button>
      {search && (
        <LiveView.Provider value={true}>
          <Gather key={search.n} sheets={search.sheets} text={search.text} sofar={[]} done={setLines} />
        </LiveView.Provider>
      )}
      {search && lines && (
        <>
          <p>
            Found <strong data-testid="search-count">{lines.length}</strong>
          </p>
          <ul>
            {lines.map((l, i) => (
              <li key={i} data-testid="search-line">
                {l}
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 37, Component: TreeSearch });

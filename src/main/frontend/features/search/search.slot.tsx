import { useCallback, useEffect, useState } from 'react';
import { useIsFetching, useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { ReadNow } from '../../query/readNow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useClaims } from '../claims/claims';
import { usePeopleDue } from '../header/totals';
import { usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { useSettings } from '../settings/settings';

/** One sheet's people, in list order, each with their names (name then aliases) and due-total as their row shows it. */
type Found = { sheetId: string; name: string; names: string[]; due: string }[];

/** Works out one sheet's people and due-totals with the same hooks as its rows, and reports them once all are read.
 *  Shows nothing. */
function SheetPeople({ sheetId, report }: { sheetId: string; report: (sheetId: string, found: Found) => void }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId);
  const due = usePeopleDue(sheetId);
  const fetching = useIsFetching({ queryKey: ['sheet', sheetId] });
  const ready = fetching === 0 && people.isSuccess && claims.isSuccess && rates.isSuccess && settings.isSuccess;
  const found: Found | null = ready
    ? people.data.map((p) => ({ sheetId, name: p.name, names: [p.name, ...(p.aliases ?? [])], due: due.get(p.id) ?? 'n/a' }))
    : null;
  const key = found === null ? null : JSON.stringify(found);
  useEffect(() => {
    if (key !== null) report(sheetId, JSON.parse(key) as Found);
  }, [report, sheetId, key]);
  return null;
}

/** The tree search: every person, over this sheet and every other sheet of its tree, whose name or an alias contains the
 *  typed text (case ignored), one line each as `<sheet id> <name> <due-total>`: this sheet's people first in list order,
 *  then the other sheets' by sheet id. The sheets are read as they are now, afresh on each press. */
function TreeSearch({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [found, setFound] = useState<Record<string, Found>>({});
  const search = useMutation({
    mutationFn: async (typed: string) => {
      const others = await getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/tree`);
      const sheets = [sheetId, ...others];
      // re-read every sheet so what another user changed is seen
      await Promise.all(sheets.map((id) => queryClient.refetchQueries({ queryKey: ['sheet', id] })));
      return { typed: typed.trim().toLowerCase(), sheets };
    },
  });
  const report = useCallback((id: string, f: Found) => setFound((all) => ({ ...all, [id]: f })), []);
  const done = search.data !== undefined && search.data.sheets.every((id) => found[id] !== undefined);
  const lines = done
    ? search.data!.sheets.flatMap((id) =>
        found[id].filter((p) => p.names.some((n) => n.toLowerCase().includes(search.data!.typed))),
      )
    : [];

  return (
    <section data-testid="tree-search">
      <h3>Search the tree</h3>
      <input
        data-testid="search-name"
        placeholder="Name or alias"
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          search.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="search"
        disabled={search.isPending}
        onClick={() => {
          search.reset();
          setFound({});
          search.mutate(text);
        }}
      >
        Search
      </button>
      {search.isError && <p data-testid="search-error">Could not search the tree.</p>}
      {search.data !== undefined && (
        <ReadNow.Provider value={true}>
          {search.data.sheets.map((id) => (
            <SheetPeople key={id} sheetId={id} report={report} />
          ))}
        </ReadNow.Provider>
      )}
      {done && (
        <>
          {' '}
          Found <span data-testid="search-count">{lines.length}</span>
        </>
      )}
      <ul>
        {lines.map((p, i) => (
          <li key={i} data-testid="search-line">
            {p.sheetId} {p.name} {p.due}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 89, Component: TreeSearch });

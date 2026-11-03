import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { useAliases, usePeople } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** One search: its text, and the sheets it runs over (this sheet first, then the others of its tree by id). */
type Run = { n: number; text: string; sheets: string[] };

/** Works out one sheet's matching lines (`<sheet id> <name> <due-total>`, in list order, retired people
 *  included) with the same hooks its own screen uses, and hands them up once all is read; shows nothing. */
function SheetMatches({ sheetId, text, onLines }: { sheetId: string; text: string; onLines: (id: string, lines: string[]) => void }) {
  const people = usePeople(sheetId);
  const aliases = useAliases(sheetId);
  const ready = [people, aliases, useClaims(sheetId), useRates(sheetId), useSheetToday(sheetId), useSettings(sheetId)]
    .every((q) => q.isSuccess);
  const due = useDueTotals(sheetId);
  const wanted = text.trim().toLowerCase();
  const lines = (people.data ?? [])
    .filter((p) => [p.name, ...(aliases.data ?? []).filter((a) => a.personId === p.id).map((a) => a.alias)]
      .some((n) => n.toLowerCase().includes(wanted)))
    .map((p) => `${sheetId} ${p.name} ${due.get(p.id) ?? 'n/a'}`);
  const joined = lines.join('\n');
  useEffect(() => {
    if (ready) onLines(sheetId, joined === '' ? [] : joined.split('\n'));
  }, [ready, sheetId, joined, onLines]);
  return null;
}

/** A search box and button listing, over this sheet and every other sheet of its tree, every person whose name or
 *  an alias contains the typed text (case ignored): one line per person, `<sheet id> <name> <due-total>`, this
 *  sheet's people first in list order, then the other sheets' by sheet id. The other sheets are re-read from the
 *  store on each press; the count is cleared while a search runs. */
function TreeSearch({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [run, setRun] = useState<Run | null>(null);
  const [found, setFound] = useState<{ n: number; lines: Record<string, string[]> }>({ n: 0, lines: {} });
  const [counter, setCounter] = useState(0);
  const [onLines] = useState(() => (id: string, lines: string[]) =>
    setFound((f) => ({ ...f, lines: { ...f.lines, [id]: lines } })));

  const search = async () => {
    const n = counter + 1;
    setCounter(n);
    setRun(null);
    setFound({ n, lines: {} });
    const { sheets } = await getJson<{ sheets: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/tree`);
    for (const id of sheets) queryClient.removeQueries({ queryKey: ['sheet', id] });
    setRun({ n, text, sheets: [sheetId, ...sheets] });
  };

  const done = run !== null && found.n === run.n && run.sheets.every((id) => found.lines[id] !== undefined);
  const lines = done ? run.sheets.flatMap((id) => found.lines[id]) : [];

  return (
    <section>
      <input
        type="text"
        placeholder="Search the tree"
        data-testid="search-name"
        value={text}
        onChange={(e) => setText(e.target.value)}
      />{' '}
      <button type="button" data-testid="search" onClick={() => void search()}>
        Search
      </button>{' '}
      <span data-testid="search-count">{done ? lines.length : ''}</span>
      {run?.sheets.map((id) => (
        <SheetMatches key={`${run.n}-${id}`} sheetId={id} text={run.text} onLines={onLines} />
      ))}
      <ul>
        {lines.map((line, i) => (
          <li key={i} data-testid="search-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 9, Component: ({ sheetId }) => <TreeSearch sheetId={sheetId} /> });

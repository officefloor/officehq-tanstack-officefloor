import { useCallback, useEffect, useState } from 'react';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useClaims } from '../claims/useClaims';
import { useSheetTotals } from '../claims/useSheetTotals';
import { usePeople } from '../people/usePeople';
import { showDue, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheet } from '../sheet/useSheet';

/** One result line: `<sheet id> <name> <due-total>`. */
type Found = string[];

/** One sheet's people whose name or an alias contains `text` (case ignored), in list order, each with
 *  their due-total as their row shows it, reported once everything it is worked out from has been
 *  re-read. Renders nothing. */
function SheetMatches({ sheet, text, report }: { sheet: string; text: string; report: (sheet: string, found: Found | null) => void }) {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet);
  const today = useSheet(sheet);
  const { perPerson } = useSheetTotals(sheet);
  const queries = [people, claims, rates, settings, today];
  const ready = queries.every((q) => q.isSuccess && !q.isFetching);
  const needle = text.toLowerCase();
  const lines = perPerson
    .filter(({ person }) => [person.name, ...person.aliases].some((n) => n.toLowerCase().includes(needle)))
    .map(({ person, due }) => `${sheet} ${person.name} ${showDue(due)}`);
  const key = lines.join('\n');
  useEffect(() => {
    report(sheet, ready ? key.split('\n').filter((l) => l !== '') : null);
  }, [sheet, ready, key, report]);
  return null;
}

/**
 * The tree search: over this sheet and every other sheet of its tree (its ancestors and descendants),
 * every person whose name or an alias contains the typed text (case ignored), one line per person,
 * `<sheet id> <name> <due-total>`: this sheet's people first in list order, then the other sheets'
 * by sheet id. Each press re-reads the sheets; the count is blank until the lines are in.
 */
function TreeSearch({ sheet }: { user: string; sheet: string }) {
  const [text, setText] = useState('');
  const [run, setRun] = useState<{ n: number; sheet: string; text: string; sheets: string[] } | null>(null);
  const [found, setFound] = useState<Record<string, Found | null>>({});
  const report = useCallback((id: string, f: Found | null) => {
    setFound((was) => ({ ...was, [id]: f }));
  }, []);
  useEffect(() => {
    setRun(null);
    setFound({});
  }, [sheet]);

  const search = async () => {
    const n = (run?.n ?? 0) + 1;
    setRun(null);
    setFound({});
    const others = await getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tree`);
    setRun({ n, sheet, text, sheets: [sheet, ...others.filter((s) => s !== sheet)] });
  };

  const current = run !== null && run.sheet === sheet ? run : null;
  const all = current === null ? [] : current.sheets.map((id) => found[id] ?? null);
  const ready = current !== null && all.every((f) => f !== null);
  const lines = ready ? all.flatMap((f) => f ?? []) : [];

  return (
    <section data-testid="tree-search">
      {current !== null &&
        current.sheets.map((id) => <SheetMatches key={`${current.n}-${id}`} sheet={id} text={current.text} report={report} />)}
      <input data-testid="search-name" value={text} onChange={(e) => setText(e.target.value)} placeholder="Search the tree" />
      <button data-testid="search" type="button" onClick={() => void search()}>
        Search
      </button>{' '}
      <strong data-testid="search-count">{ready ? lines.length : ''}</strong>
      <ul>
        {lines.map((l, i) => (
          <li key={i} data-testid="search-line">
            {l}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 22, Component: TreeSearch });

import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { useAliases } from '../aliases/aliases';
import { useClaims, usePersonDueCentsOf } from '../claims/claims';
import { useRates, showDue } from '../rates/rates';
import { useSheet } from '../sheet/sheet';
import { useSettings } from '../settings/settings';
import { LiveFigures } from '../../query/live';

// One sheet's matching people, each as "<sheet id> <name> <due-total>", in list order. Renders nothing; it reports
// them up once every read its lines are figured from has come back from the store (it mounts afresh on each search,
// so a stale read is fetched again first).
function SheetMatches({ sheet, text, report }: { sheet: string; text: string; report: (sheet: string, lines: string[]) => void }) {
  const reads = [usePeople(sheet), useAliases(sheet), useClaims(sheet), useRates(sheet), useSheet(sheet), useSettings(sheet)];
  const ready = reads.every((r) => r.isSuccess && !r.isFetching);
  const people = reads[0].data as ReturnType<typeof usePeople>['data'];
  const aliases = reads[1].data as ReturnType<typeof useAliases>['data'];
  const dueOf = usePersonDueCentsOf(sheet);
  const needle = text.toLowerCase();
  const lines =
    ready && people && aliases
      ? people
          .filter(
            (p) =>
              p.name.toLowerCase().includes(needle) ||
              aliases.some((a) => a.person === p.id && a.alias.toLowerCase().includes(needle)),
          )
          .map((p) => `${sheet} ${p.name} ${showDue(dueOf(p))}`)
      : null;
  const key = lines === null ? null : JSON.stringify(lines);
  useEffect(() => {
    if (key !== null) report(sheet, JSON.parse(key) as string[]);
  }, [sheet, key, report]);
  return null;
}

type Search = { run: number; text: string; sheets: string[] | null };

// Searches this sheet and every other sheet of its tree for the people whose name or an alias contains the typed text
// (ignoring case): one line each, this sheet's people first in list order, then the other sheets' by sheet id. The
// figures are the sheets' as they stand now, even while the screen shows the sheet as of a date.
function TreeSearch({ sheet }: { sheet: string; user: string }) {
  const [text, setText] = useState('');
  const [search, setSearch] = useState<Search | null>(null);
  const [found, setFound] = useState<Map<string, string[]>>(new Map());
  const runs = useRef(0);

  useEffect(() => {
    setSearch(null);
    setFound(new Map());
  }, [sheet]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const run = ++runs.current;
    setFound(new Map());
    setSearch({ run, text, sheets: null });
    getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tree`).then(
      (sheets) => setSearch((s) => (s?.run === run ? { ...s, sheets } : s)),
      () => setSearch((s) => (s?.run === run ? { ...s, sheets: [sheet] } : s)),
    );
  };

  // Only the current search's sheets are mounted, so every report belongs to it.
  const report = useCallback((s: string, lines: string[]) => setFound((prev) => new Map(prev).set(s, lines)), []);

  const sheets = search?.sheets ?? null;
  const done = sheets !== null && sheets.every((s) => found.has(s));
  const lines = done ? sheets.flatMap((s) => found.get(s) ?? []) : [];

  return (
    <section>
      <form onSubmit={submit}>
        <input data-testid="search-name" placeholder="name or alias" value={text} onChange={(e) => setText(e.target.value)} />{' '}
        <button type="submit" data-testid="search">
          Search tree
        </button>{' '}
        Found <strong data-testid="search-count">{done ? lines.length : ''}</strong>
      </form>
      {search && sheets && (
        <LiveFigures.Provider value={true}>
          {sheets.map((s) => (
            <SheetMatches key={`${search.run}-${s}`} sheet={s} text={search.text} report={report} />
          ))}
        </LiveFigures.Provider>
      )}
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

export const contribution = SheetPanels.fill({ order: -8, Component: TreeSearch });

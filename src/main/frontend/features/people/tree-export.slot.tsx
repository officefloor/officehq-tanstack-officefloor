import { useCallback, useEffect, useRef, useState } from 'react';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from './people';
import { useAliases } from '../aliases/aliases';
import { useClaims, usePersonDueCentsOf } from '../claims/claims';
import { useRates, showDue } from '../rates/rates';
import { useSheet } from '../sheet/sheet';
import { useSettings } from '../settings/settings';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// One sheet's people as CSV lines "<sheet>,<name>,<grade>,<due-total>", in list order, retired people included.
// Renders nothing; it reports them up once every read they are figured from has come back from the store (it mounts
// afresh on each export, so a stale read is fetched again first).
function SheetLines({ sheet, report }: { sheet: string; report: (sheet: string, lines: string[]) => void }) {
  const reads = [usePeople(sheet), useAliases(sheet), useClaims(sheet), useRates(sheet), useSheet(sheet), useSettings(sheet)];
  const ready = reads.every((r) => r.isSuccess && !r.isFetching);
  const people = reads[0].data as ReturnType<typeof usePeople>['data'];
  const dueOf = usePersonDueCentsOf(sheet);
  const lines =
    ready && people
      ? people.map((p) => [sheet, p.name, p.grade, showDue(dueOf(p))].map(csvValue).join(','))
      : null;
  const key = lines === null ? null : JSON.stringify(lines);
  useEffect(() => {
    if (key !== null) report(sheet, JSON.parse(key) as string[]);
  }, [sheet, key, report]);
  return null;
}

type Run = { run: number; sheets: string[] | null };

// The button that downloads the people of the sheet's whole tree as CSV: this sheet's people first in list order,
// then those of the tree's other sheets by sheet id (see the tree endpoint), each with the due-total its row shows.
function ExportTree({ sheet }: { sheet: string; user: string }) {
  const [run, setRun] = useState<Run | null>(null);
  const [found, setFound] = useState<Map<string, string[]>>(new Map());
  const runs = useRef(0);

  useEffect(() => {
    setRun(null);
    setFound(new Map());
  }, [sheet]);

  const start = () => {
    const n = ++runs.current;
    setFound(new Map());
    setRun({ run: n, sheets: null });
    getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tree`).then(
      (sheets) => setRun((r) => (r?.run === n ? { ...r, sheets } : r)),
      () => setRun((r) => (r?.run === n ? { ...r, sheets: [sheet] } : r)),
    );
  };

  const report = useCallback((s: string, lines: string[]) => setFound((prev) => new Map(prev).set(s, lines)), []);

  const sheets = run?.sheets ?? null;
  const done = sheets !== null && sheets.every((s) => found.has(s));

  useEffect(() => {
    if (!done || !sheets) return;
    const lines = ['sheet,name,grade,due-total', ...sheets.flatMap((s) => found.get(s) ?? [])];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `tree-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setRun(null);
    setFound(new Map());
  }, [done, sheets, found, sheet]);

  return (
    <p>
      <button type="button" data-testid="export-tree" disabled={run !== null} onClick={start}>
        Export tree people (CSV)
      </button>
      {run && sheets && sheets.map((s) => <SheetLines key={`${run.run}-${s}`} sheet={s} report={report} />)}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 12, Component: ExportTree });

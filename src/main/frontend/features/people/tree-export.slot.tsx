import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { LiveView, useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey, useSheet, useSheetToday } from '../sheet/sheet';
import { gradeOn, usePeople } from './people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useSettings } from '../settings/settings';
import { usePersonDue } from '../rates/due.slot';

/** Under the sheet's key, so loading the sheet again re-reads its whole tree. */
const wholeTreeKey = (sheet: string) => [...sheetKey(sheet), 'whole-tree'] as const;

/** A CSV value: wrapped in double quotes, inner quotes doubled, when it holds a comma, quote or line break. */
const cell = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

type Export = { sheets: string[]; n: number };

/** Reads the first sheet's people (each with their grade in force on the sheet's today and their due-total as
 *  their row shows it), adds their lines to those so far, then the rest's, one sheet per component so each reads
 *  its sheet with the same hooks; reports the lines once every sheet is read in full. */
function Gather({ sheets, sofar, done }: { sheets: string[]; sofar: string[][]; done: (lines: string[][]) => void }) {
  const [first, ...rest] = sheets;
  const people = usePeople(first);
  const claims = useClaims(first);
  const rates = useRates(first);
  const settings = useSettings(first);
  const sheet = useSheet(first);
  const today = useSheetToday(first);
  const dueOf = usePersonDue(first);
  const ready = !!(people.data && claims.data && rates.data && settings.data && sheet.data);
  const lines = ready
    ? [...sofar, ...people.data!.map((p) => [first, p.name, gradeOn(p.grades, today), dueOf(p)])]
    : sofar;
  const last = ready && rest.length === 0;
  const key = JSON.stringify(lines);
  useEffect(() => {
    if (last) done(JSON.parse(key) as string[][]);
  }, [last, key]); // eslint-disable-line react-hooks/exhaustive-deps
  if (ready && rest.length > 0) return <Gather sheets={rest} sofar={lines} done={done} />;
  return null;
}

// Downloads the people of the sheet's whole tree as CSV: sheet, name, grade in force on their sheet's today and
// due-total as their row shows it, one line per person (retired people included), this sheet's people first in
// list order, then the other sheets' by sheet id, as they are now.
function ExportTree() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const [run, setRun] = useState<Export | null>(null);
  const [n, setN] = useState(0);

  useEffect(() => setRun(null), [sheet]);

  const start = async () => {
    const at = n + 1;
    setN(at);
    setRun(null);
    const all = await queryClient.fetchQuery({
      queryKey: wholeTreeKey(sheet),
      queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/whole-tree`),
      staleTime: 0,
    });
    const others = all.filter((s) => s !== sheet).sort();
    await Promise.all(others.map((s) => queryClient.refetchQueries({ queryKey: sheetKey(s) })));
    setRun({ sheets: [sheet, ...others], n: at });
  };

  const download = (lines: string[][]) => {
    setRun(null);
    const csv = [['sheet', 'name', 'grade', 'due-total'], ...lines].map((l) => l.map(cell).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `tree-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  if (!user || !sheet) return null;
  return (
    <p>
      <button type="button" data-testid="export-tree" disabled={run !== null} onClick={() => void start()}>
        Export tree people (CSV)
      </button>
      {run && (
        <LiveView.Provider value={true}>
          <Gather key={run.n} sheets={run.sheets} sofar={[]} done={download} />
        </LiveView.Provider>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 21.1, Component: ExportTree });

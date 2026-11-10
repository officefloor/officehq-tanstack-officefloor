import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, usePeople } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { usePersonPeriodCarried, usePersonPeriodDueTotals } from '../periods/usePeriodDueTotals';
import { useFigure } from '../locale/useLocale';
import { usePools } from '../pools/usePools';
import { cutTotalsOf } from '../cuts/useCutTotals';
import { dueTotalsOf, useDueTotals } from '../rates/useDueTotals';
import { rateFor, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { downloadCsv, fresh } from './csv';

/** A button that downloads the people as a CSV file: a header line, then one line per person in list
 *  order, retired people included, with the grade in force on the sheet's today, the retired-from date
 *  (blank when not retired) and the due-total as shown. Lines end with a line feed. While an as-of date is
 *  set, the people and their due-totals are as the sheet stood then; while a preview date is set, the due-totals
 *  are the preview figures, worked out with that date as the sheet's today. */
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId);
  const today = useSheetToday(sheetId);
  const pools = usePools(sheetId);

  // Re-read everything first: the screen's as-of or preview date may have changed since it was loaded.
  const download = async () => {
    const [ps, cs, rs, ss, td, pl] = await Promise.all([fresh(people), fresh(claims), fresh(rates), fresh(settings), fresh(today), fresh(pools)]);
    const dueTotals = dueTotalsOf(ps, cs, rs, ss, td?.today, cutTotalsOf(ps, pl));
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of ps ?? []) {
      lines.push([p.name, p.grade, p.retiredFrom ?? '', dueTotals.get(p.id) ?? '']);
    }
    downloadCsv(lines, `people-${sheetId}.csv`);
  };

  return (
    <button type="button" data-testid="export-people" onClick={() => void download()}>
      Export people (CSV)
    </button>
  );
}

/** A button that downloads the claims as a CSV file: a header line, then one line per claim, people in
 *  list order (retired people included) and each person's claims oldest first, with the number, period
 *  and due as the claim's row shows them (blank number or period, 'n/a' due, as shown). While an as-of date
 *  is set, the claims and their dues are as the sheet stood then; while a preview date is set, the dues are the
 *  preview figures, worked out with that date as the sheet's today. */
function ExportClaims({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const periods = usePeriods(sheetId);
  const settingsQ = useSettings(sheetId);
  const todayQ = useSheetToday(sheetId);

  // Re-read everything first: the screen's as-of or preview date may have changed since it was loaded.
  const download = async () => {
    const [ps, cs, rs, pds, settings, td] = await Promise.all(
      [fresh(people), fresh(claims), fresh(rates), fresh(periods), fresh(settingsQ), fresh(todayQ)]);
    const today = td?.today;
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of ps ?? []) {
      const mine = (cs ?? []).filter((c) => c.personId === p.id).reverse();
      for (const c of mine) {
        const grade = gradeOn(p, c.date);
        lines.push([
          p.name,
          c.number ?? '',
          c.date,
          c.amount,
          claimDueNow(c, cs, rateFor(rs, grade, c.date, c.added), settings, today, grade),
          c.status,
          periodOf(pds, c.dueBy)?.name ?? '',
        ]);
      }
    }
    downloadCsv(lines, `claims-${sheetId}.csv`);
  };

  return (
    <button type="button" data-testid="export-claims" onClick={() => void download()}>
      Export claims (CSV)
    </button>
  );
}

/** A button that downloads the pools as a CSV file: a header line, then one line per member, pools in the
 *  order they were added and each pool's members in the order they were added, a pool member as
 *  pool:<name>, and the cut as shown (blank when none). While an as-of date is set, the pools and their cuts
 *  are as the sheet stood then; while a preview date is set, the cuts are the preview figures. */
function ExportPools({ sheetId }: { sheetId: string }) {
  const pools = usePools(sheetId);

  // Re-read the pools first: a cut follows people, settings and other pools changed since they were loaded.
  const download = async () => {
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of [...((await fresh(pools)) ?? [])].sort((a, b) => a.added - b.added)) {
      for (const m of p.members) {
        lines.push([p.name, m.pool === null ? m.name : `pool:${m.pool}`, m.share, m.cut ?? '']);
      }
    }
    downloadCsv(lines, `pools-${sheetId}.csv`);
  };

  return (
    <button type="button" data-testid="export-pools" onClick={() => void download()}>
      Export pools (CSV)
    </button>
  );
}

/** A button that downloads the period figures as a CSV file: a header line, then one line per person and
 *  period, people in list order and periods in the order they were added, with each person's due-total and
 *  carried figure for the period as their row shows them. */
function ExportPeriods({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const people = usePeople(sheetId);
  const periods = usePeriods(sheetId);
  const totals = usePersonPeriodDueTotals(sheetId);
  const carried = usePersonPeriodCarried(sheetId);

  const download = () => {
    const lines = [['person', 'period', 'figure', 'carried']];
    const added = [...(periods.data ?? [])].sort((a, b) => a.id - b.id);
    for (const p of people.data ?? []) {
      for (const pd of added) {
        lines.push([
          p.name,
          pd.name,
          fig(totals.get(p.id)?.get(pd.name) ?? ''),
          fig(carried.get(p.id)?.get(pd.name) ?? ''),
        ]);
      }
    }
    downloadCsv(lines, `periods-${sheetId}.csv`);
  };

  return (
    <button type="button" data-testid="export-periods" onClick={download}>
      Export periods (CSV)
    </button>
  );
}

/** One tree export: the sheets it runs over (this sheet first, then the others of its tree by id). */
type TreeRun = { n: number; sheets: string[] };

/** Works out one sheet's lines (sheet id, name, grade, due-total as shown, 'n/a' when none; in list order, retired
 *  people included) with the same hooks its own screen uses, and hands them up once all is read; shows nothing. */
function SheetPeopleLines({ sheetId, onLines }: { sheetId: string; onLines: (id: string, lines: string[][]) => void }) {
  const people = usePeople(sheetId);
  const ready = [people, useClaims(sheetId), useRates(sheetId), useSheetToday(sheetId), useSettings(sheetId), usePools(sheetId)]
    .every((q) => q.isSuccess);
  const due = useDueTotals(sheetId);
  const lines = (people.data ?? []).map((p) => [sheetId, p.name, p.grade, due.get(p.id) ?? 'n/a']);
  // Joined into a string so the effect below only fires when a line changes.
  const joined = JSON.stringify(lines);
  useEffect(() => {
    if (ready) onLines(sheetId, JSON.parse(joined) as string[][]);
  }, [ready, sheetId, joined, onLines]);
  return null;
}

/** A button that downloads the people of the whole tree as a CSV file: a header line, then one line per person,
 *  this sheet's people first in list order, then the other sheets' of its tree by sheet id. The other sheets are
 *  re-read from the store on each press. */
function ExportTree({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [run, setRun] = useState<TreeRun | null>(null);
  const [found, setFound] = useState<{ n: number; lines: Record<string, string[][]> }>({ n: 0, lines: {} });
  const [counter, setCounter] = useState(0);
  const [onLines] = useState(() => (id: string, lines: string[][]) =>
    setFound((f) => ({ ...f, lines: { ...f.lines, [id]: lines } })));

  const start = async () => {
    const n = counter + 1;
    setCounter(n);
    setRun(null);
    setFound({ n, lines: {} });
    const { sheets } = await getJson<{ sheets: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/tree`);
    for (const id of sheets) queryClient.removeQueries({ queryKey: ['sheet', id] });
    setRun({ n, sheets: [sheetId, ...sheets] });
  };

  const done = run !== null && found.n === run.n && run.sheets.every((id) => found.lines[id] !== undefined);
  useEffect(() => {
    if (!done || run === null) return;
    downloadCsv([['sheet', 'name', 'grade', 'due-total'], ...run.sheets.flatMap((id) => found.lines[id])], `tree-${sheetId}.csv`);
    setRun(null);
  }, [done, run, found, sheetId]);

  return (
    <>
      <button type="button" data-testid="export-tree" onClick={() => void start()}>
        Export tree (CSV)
      </button>
      {run?.sheets.map((id) => (
        <SheetPeopleLines key={`${run.n}-${id}`} sheetId={id} onLines={onLines} />
      ))}
    </>
  );
}

export const contribution = SheetBody.fill({
  order: 15,
  Component: ({ sheetId }) => (
    <section data-testid="export">
      <ExportPeople sheetId={sheetId} />
      <ExportClaims sheetId={sheetId} />
      <ExportPools sheetId={sheetId} />
      <ExportPeriods sheetId={sheetId} />
      <ExportTree sheetId={sheetId} />
    </section>
  ),
});

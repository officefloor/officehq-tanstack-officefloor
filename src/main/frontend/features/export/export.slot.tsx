import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, usePeople } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { usePools } from '../pools/usePools';
import { useDueTotals } from '../rates/useDueTotals';
import { rateFor, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote
 *  or a line break. */
function csvValue(v: string): string {
  return /[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
}

/** Download these lines as a CSV file of this name, each line ending with a line feed. */
function downloadCsv(lines: string[][], filename: string) {
  const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** A button that downloads the people as a CSV file: a header line, then one line per person in list
 *  order, retired people included, with the grade in force on the sheet's today, the retired-from date
 *  (blank when not retired) and the due-total as shown. Lines end with a line feed. */
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const dueTotals = useDueTotals(sheetId);

  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people.data ?? []) {
      lines.push([p.name, p.grade, p.retiredFrom ?? '', dueTotals.get(p.id) ?? '']);
    }
    downloadCsv(lines, `people-${sheetId}.csv`);
  };

  return (
    <button type="button" data-testid="export-people" onClick={download}>
      Export people (CSV)
    </button>
  );
}

/** A button that downloads the claims as a CSV file: a header line, then one line per claim, people in
 *  list order (retired people included) and each person's claims oldest first, with the number, period
 *  and due as the claim's row shows them (blank number or period, 'n/a' due, as shown). */
function ExportClaims({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const periods = usePeriods(sheetId);
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;

  const download = () => {
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people.data ?? []) {
      const mine = (claims.data ?? []).filter((c) => c.personId === p.id).reverse();
      for (const c of mine) {
        const grade = gradeOn(p, c.date);
        lines.push([
          p.name,
          c.number ?? '',
          c.date,
          c.amount,
          claimDueNow(c, claims.data, rateFor(rates.data, grade, c.date), settings, today, grade),
          c.status,
          periodOf(periods.data, c.dueBy)?.name ?? '',
        ]);
      }
    }
    downloadCsv(lines, `claims-${sheetId}.csv`);
  };

  return (
    <button type="button" data-testid="export-claims" onClick={download}>
      Export claims (CSV)
    </button>
  );
}

/** A button that downloads the pools as a CSV file: a header line, then one line per member, pools in the
 *  order they were added and each pool's members in the order they were added, a pool member as
 *  pool:<name>, and the cut as shown (blank when none). */
function ExportPools({ sheetId }: { sheetId: string }) {
  const pools = usePools(sheetId);

  // Re-read the pools first: a cut follows people, settings and other pools changed since they were loaded.
  const download = async () => {
    const fresh = (await pools.refetch()).data ?? pools.data ?? [];
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of [...fresh].sort((a, b) => a.added - b.added)) {
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

export const contribution = SheetBody.fill({
  order: 15,
  Component: ({ sheetId }) => (
    <section data-testid="export">
      <ExportPeople sheetId={sheetId} />
      <ExportClaims sheetId={sheetId} />
      <ExportPools sheetId={sheetId} />
    </section>
  ),
});

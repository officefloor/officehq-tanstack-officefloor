import { SheetBody } from '../../slots/defs/sheetBody';
import { gradeOn, usePeople } from '../people/people.slot';
import { useClaims } from '../claims/useClaims';
import { periodOn, usePeriods } from '../periods/periods.slot';
import { NA, useDueTotals } from '../rates/rates.slot';
import { claimDueCents, showHundredths, useRates } from '../rates/useRates';
import { useFloorCents, useLate } from '../access/useAccess';
import { usePools } from '../pools/pools.slot';

/** A CSV value: wrapped in double quotes, with any quote inside doubled, when it holds a comma, a quote or a line break. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** Downloads the lines as a CSV file of that name, each line ending with a line feed. */
function downloadCsv(name: string, lines: string[][]) {
  const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// The button that downloads the people as a CSV file: a header line, then one line per person in list order,
// retired people included, with the grade in force today, the retired-from date (or nothing) and the due-total as
// its row shows it. Lines end with a line feed.
function ExportPeople({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const totals = useDueTotals(sheetId);

  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people.data ?? []) {
      const total = totals.get(p.id);
      lines.push([p.name, p.grade, p.retiredFrom ?? '', total == null ? 'n/a' : showHundredths(total)]);
    }
    downloadCsv(`people-${sheetId}.csv`, lines);
  };

  return (
    <section data-testid="export-panel">
      <button type="button" data-testid="export-people" disabled={!people.data} onClick={download}>
        Export people (CSV)
      </button>{' '}
      <ExportClaims sheetId={sheetId} />{' '}
      <ExportPools sheetId={sheetId} />
    </section>
  );
}

// The button that downloads the claims as a CSV file: a header line, then one line per claim, people in list order (retired
// people included) and each person's claims oldest first, with the number, due, status and period as its row shows them.
function ExportClaims({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const periods = usePeriods(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);

  const download = () => {
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people.data ?? []) {
      // The claims come newest first.
      const mine = (claims.data ?? []).filter((c) => c.personId === p.id).reverse();
      for (const c of mine) {
        const due = claimDueCents(rates.data ?? [], gradeOn(p, c.date), c, floor, late);
        lines.push([p.name, c.number ?? '', c.date, Number(c.amount).toFixed(2), due == null ? NA : showHundredths(due), c.status,
          periodOn(periods.data ?? [], c.dueBy)?.name ?? '']);
      }
    }
    downloadCsv(`claims-${sheetId}.csv`, lines);
  };

  return (
    <button type="button" data-testid="export-claims" disabled={!people.data || !claims.data} onClick={download}>
      Export claims (CSV)
    </button>
  );
}

// The button that downloads the pools as a CSV file: a header line, then one line per member, pools in the order added
// and each pool's members in the order added, a member pool as pool:<name>, with the share and the cut as its line shows
// them (nothing when it has none).
function ExportPools({ sheetId }: { sheetId: string }) {
  const pools = usePools(sheetId);

  const download = async () => {
    const { data } = await pools.refetch();
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of data ?? []) {
      for (const m of p.members) {
        lines.push([p.name, m.pool === null ? m.name : `pool:${m.pool}`, m.share, m.cut ?? '']);
      }
    }
    downloadCsv(`pools-${sheetId}.csv`, lines);
  };

  return (
    <button type="button" data-testid="export-pools" disabled={!pools.data} onClick={() => void download()}>
      Export pools (CSV)
    </button>
  );
}

export const contribution = SheetBody.fill({ order: 11, Component: ExportPeople });

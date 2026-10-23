import { SheetBody } from '../../slots/defs/sheet';
import { gradeOn, usePeople } from '../people/people';
import { periodOf, usePeriods } from '../periods/periods';
import { claimDue, rateOn, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { dueBy, useClaims } from './claims';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line
 *  break, else as it is (as the people export writes it). */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** The button that downloads the claims as a CSV file: a header line, then one line per claim, people in list order
 *  (retired people included) and each person's claims oldest first, with its person's name, number (or nothing),
 *  date, amount, due, status and period (or nothing) as its row shows them; each line ends with a line feed. While the
 *  screen previews a date, the dues are figured on it (late charges included), as the rows show them. */
function ExportClaims({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const download = () => {
    const lines = [
      ['person', 'number', 'date', 'amount', 'due', 'status', 'period'],
      // The claims come newest first: each person's are reversed to oldest first.
      ...people.flatMap((p) => claims.filter((c) => c.personId === p.id).reverse().map((c) => [
        p.name,
        c.number ?? '',
        c.date,
        c.amount,
        claimDue(c, rateOn(rates, gradeOn(p, c.date), c.date, c.id), floor, late),
        c.status,
        periodOf(periods, dueBy(c))?.name ?? '',
      ])),
    ];
    const csv = lines.map((values) => values.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `claims-${sheetId}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  };

  return (
    <p>
      <button type="button" data-testid="export-claims" onClick={download}>
        Export claims (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 11.1, Component: ExportClaims });

import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { personPeriodCarried, personPeriodDueTotal, usePeriods } from './periods';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line
 *  break, else as it is. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** The button that downloads the period figures as a CSV file: a header line, then one line per person and period,
 *  people in list order (retired people included) and periods in the order added, with the person's due-total and
 *  carried figure for the period as their row shows them (see personPeriodDueTotal and personPeriodCarried); each
 *  line ends with a line feed. */
function ExportPeriods({ sheetId }: { sheetId: string; user: string }) {
  const periods = usePeriods(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const download = () => {
    // The periods come earliest first: they are put in the order added (earliest first among any not numbered).
    const added = periods
      .map((p, i) => ({ p, i }))
      .sort((x, y) => (x.p.added ?? x.i) - (y.p.added ?? y.i) || x.i - y.i)
      .map((x) => x.p);
    const lines = [
      ['person', 'period', 'figure', 'carried'],
      ...people.flatMap((person) =>
        added.map((p) => [
          person.name,
          p.name,
          personPeriodDueTotal(p, person, claims, people, rates, floor, late),
          personPeriodCarried(p, periods, person, claims, people, rates, floor, late),
        ]),
      ),
    ];
    const csv = lines.map((values) => values.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `periods-${sheetId}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  };

  return (
    <p>
      <button type="button" data-testid="export-periods" onClick={download}>
        Export period figures (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 6.3, Component: ExportPeriods });

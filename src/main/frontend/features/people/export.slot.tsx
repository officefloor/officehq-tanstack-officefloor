import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { personDueTotal, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { gradeOn, usePeople } from './people';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line
 *  break, else as it is. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** The button that downloads the people as a CSV file: a header line, then one line per person in list order,
 *  retired people included, with their grade in force on the sheet's today, the date they are retired from (or
 *  nothing) and their due-total as shown on their row; each line ends with a line feed. */
function ExportPeople({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);

  const download = () => {
    const lines = [
      ['name', 'grade', 'retired-from', 'due-total'],
      ...people.map((p) => [
        p.name,
        gradeOn(p, today),
        p.retiredFrom ?? '',
        personDueTotal(p, claims, rates, today, floor, late, cap),
      ]),
    ];
    const csv = lines.map((values) => values.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `people-${sheetId}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  };

  return (
    <p>
      <button type="button" data-testid="export-people" onClick={download}>
        Export people (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 11, Component: ExportPeople });

import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople } from '../people/people.slot';
import { useDueTotals } from '../rates/rates.slot';
import { showHundredths } from '../rates/useRates';

/** A CSV value: wrapped in double quotes, with any quote inside doubled, when it holds a comma, a quote or a line break. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
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
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `people-${sheetId}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <section data-testid="export-panel">
      <button type="button" data-testid="export-people" disabled={!people.data} onClick={download}>
        Export people (CSV)
      </button>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 11, Component: ExportPeople });

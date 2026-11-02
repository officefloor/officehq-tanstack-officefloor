import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote
 *  or a line break. */
function csvValue(v: string): string {
  return /[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
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
    <section data-testid="export">
      <button type="button" data-testid="export-people" onClick={download}>
        Export people (CSV)
      </button>
    </section>
  );
}

export const contribution = SheetBody.fill({
  order: 15,
  Component: ({ sheetId }) => <ExportPeople sheetId={sheetId} />,
});

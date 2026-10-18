import { PeopleBar } from '../../slots/defs/peopleBar';
import { NA, show } from '../rates/rates';
import { useDueTotal } from '../rates/rates.slot';
import { useCurrentSheet, useSheetToday } from '../session/session';
import { gradeOn, usePeople } from './people';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote
 *  or a line break. */
const csv = (value: string) => (/[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value);

// Export the people as a CSV file: a header line, then one line per person in list order, retired
// people included, with the grade in force on the sheet's today, the retired-from date (or nothing)
// and the due-total as shown. Lines end with a line feed.
function ExportPeople() {
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const people = usePeople().data ?? [];
  const dueOf = useDueTotal();
  if (!sheet) return null;

  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const due = dueOf(p.id);
      lines.push([p.name, gradeOn(p, today) ?? p.grade ?? '', p.retiredFrom ?? '', due === undefined ? NA : show(due)]);
    }
    const text = lines.map((l) => l.map(csv).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([text], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `people-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  };

  return (
    <button type="button" data-testid="export-people" onClick={download}>
      Export CSV
    </button>
  );
}

export const contribution = PeopleBar.fill({ order: 30, Component: ExportPeople });

import { SheetPanels } from '../../slots/defs/sheetPanels';
import { live, personDueCents, useClaims } from '../claims/useClaims';
import { gradeOn, heldOnBy, usePeople } from '../people/usePeople';
import { rateOn, showDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';

/** A CSV value: wrapped in double quotes, any quote inside doubled, when it holds a comma, a quote
 *  or a line break. */
function csvValue(v: string): string {
  return /[",\r\n]/.test(v) ? `"${v.replaceAll('"', '""')}"` : v;
}

/**
 * Downloads the people as a CSV file: a header line, then one line per person in list order,
 * retired people included, with the grade in force on the sheet's today, the retired-from date
 * (empty when not retired) and the due-total as the people list shows it. Lines end with a line feed.
 */
function ExportPeople({ sheet }: { user: string; sheet: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);

  const download = () => {
    const claims = live(all, heldOnBy(people));
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const due = personDueCents(
        claims.filter((c) => c.personId === p.id),
        floor,
        rateOn(rates, gradeOn(p, today), today),
        (d) => rateOn(rates, gradeOn(p, d), d),
        late,
      );
      lines.push([p.name, gradeOn(p, today), p.retiredFrom ?? '', showDue(due)]);
    }
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `people-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
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

export const contribution = SheetPanels.fill({ order: 11, Component: ExportPeople });

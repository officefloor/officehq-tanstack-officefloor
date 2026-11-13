import { useClaims } from '../claims/claims';
import { gradeOn, usePeople } from '../people/people';
import { personDue, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useSheetToday } from '../session/sheetToday';
import { useDueRules } from '../settings/settings';

/** A CSV field: wrapped in double quotes, with any quote inside doubled, when it holds a comma, a quote or a line break. */
function csvField(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

// The button that downloads the people as a CSV file: one line per person in list order, retired
// people included, with the grade in force on the sheet's today, the retired-from date (or nothing)
// and the due-total as shown. Lines end with a line feed.
export function ExportPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  if (!user || !sheet) return null;

  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const due = personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, p.holds, rules);
      lines.push([p.name, gradeOn(p.grades, today) ?? p.grade, p.retiredFrom ?? '', due]);
    }
    const csv = lines.map((l) => l.map(csvField).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `people-${sheet}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <button type="button" data-testid="export-people" onClick={download}>
      Export people (CSV)
    </button>
  );
}

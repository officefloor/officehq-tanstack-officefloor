import { useQueryClient } from '@tanstack/react-query';
import { gradeOn, liveHolds } from '../people/people';
import { personDue } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { dueRules } from '../settings/settings';
import { freshClaims, freshPeople, freshRates, freshSettings, freshSheetInfo } from './fresh';

/** A CSV field: wrapped in double quotes, with any quote inside doubled, when it holds a comma, a quote or a line break. */
export function csvField(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

// The button that downloads the people as a CSV file: one line per person in list order, retired
// people included, with the grade in force on the sheet's today, the retired-from date (or nothing)
// and the due-total as shown. Lines end with a line feed. The sheet is re-read from the store first, so
// while the screen is shown as of a date the lines are as of that date.
export function ExportPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  if (!user || !sheet) return null;

  const download = async () => {
    const [info, people, claims, rates, settings] = await Promise.all([
      freshSheetInfo(queryClient, sheet),
      freshPeople(queryClient, sheet),
      freshClaims(queryClient, sheet),
      freshRates(queryClient, sheet),
      freshSettings(queryClient, sheet),
    ]);
    const today = info.today;
    const rules = dueRules(settings, today, claims);
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const due = personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules, {
        personId: p.id,
        claims,
        people,
      });
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

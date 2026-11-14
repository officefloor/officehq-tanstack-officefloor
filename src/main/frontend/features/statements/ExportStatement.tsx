import { useQueryClient } from '@tanstack/react-query';
import { bound } from '../claims/claims';
import { csvField } from '../export/ExportPeople';
import { freshClaims, freshPeople, freshRates, freshSettings, freshSheetInfo } from '../export/fresh';
import type { Person } from '../people/people';
import { claimDue } from '../rates/rates';
import { useCurrentSheet } from '../session/session';
import { dueRules } from '../settings/settings';

// The button that downloads a person's statement as a CSV file: a header line, then one line per claim of
// theirs their last statement bound, oldest first, with the bound figure as the claim's row shows it.
// Quoted and ended as the people export; re-read from the store first, as the other exports are.
export function ExportStatement({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const download = async () => {
    const [info, people, claims, rates, settings] = await Promise.all([
      freshSheetInfo(queryClient, sheet),
      freshPeople(queryClient, sheet),
      freshClaims(queryClient, sheet),
      freshRates(queryClient, sheet),
      freshSettings(queryClient, sheet),
    ]);
    const rules = dueRules(settings, info.today, claims);
    const grades = people.find((p) => p.id === person.id)?.grades ?? person.grades;
    const lines = [['number', 'date', 'amount', 'figure', 'status']];
    for (const c of claims.filter((c) => c.personId === person.id && bound(c)).reverse()) {
      lines.push([c.number ?? '', c.date, c.amount, claimDue(c, rates, grades, rules), c.status]);
    }
    const csv = lines.map((l) => l.map(csvField).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `statement-${sheet}-${person.id}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <button type="button" data-testid="person-export-statement" onClick={download}>
      Export statement (CSV)
    </button>
  );
}

import { useQueryClient } from '@tanstack/react-query';
import { personPeriodCarried, personPeriodDue } from '../periods/periods';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { dueRules } from '../settings/settings';
import { csvField } from './ExportPeople';
import { freshClaims, freshPeople, freshPeriods, freshPools, freshRates, freshSettings, freshSheetInfo } from './fresh';

// The button that downloads the period figures as a CSV file: one line per person and period, people in list order
// and periods in the order added, with each person's due-total and carried figure for the period as their row shows
// them (fixed while the period is closed). The sheet is re-read from the store first. Quoted and ended as the people
// export.
export function ExportPeriods() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  if (!user || !sheet) return null;

  const download = async () => {
    const [info, people, claims, rates, settings, pools, periods] = await Promise.all([
      freshSheetInfo(queryClient, sheet),
      freshPeople(queryClient, sheet),
      freshClaims(queryClient, sheet),
      freshRates(queryClient, sheet),
      freshSettings(queryClient, sheet),
      freshPools(queryClient, sheet),
      freshPeriods(queryClient, sheet),
    ]);
    const rules = dueRules(settings, info.today, claims, pools);
    const added = [...periods].sort((a, b) => (a.added ?? 0) - (b.added ?? 0));
    const lines = [['person', 'period', 'figure', 'carried']];
    for (const person of people) {
      const own = claims.filter((c) => c.personId === person.id);
      for (const p of added) {
        lines.push([person.name, p.name, personPeriodDue(p, person, own, rates, rules), personPeriodCarried(p, periods, person, own, rates, rules)]);
      }
    }
    const csv = lines.map((l) => l.map(csvField).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `periods-${sheet}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <button type="button" data-testid="export-periods" onClick={download}>
      Export periods (CSV)
    </button>
  );
}

import { useQueryClient } from '@tanstack/react-query';
import { periodOf } from '../periods/periods';
import { claimDue } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { dueRules } from '../settings/settings';
import { csvField } from './ExportPeople';
import { freshClaims, freshPeople, freshPeriods, freshRates, freshSettings, freshSheetInfo } from './fresh';

// The button that downloads the claims as a CSV file: people in list order (retired people included)
// and each person's claims oldest first, with the number, due and period as the claim's row shows
// them (an empty number or period, or n/a, as it is). Quoted and ended as the people export; re-read
// from the store first, as the people export is.
export function ExportClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  if (!user || !sheet) return null;

  const download = async () => {
    const [info, people, claims, rates, periods, settings] = await Promise.all([
      freshSheetInfo(queryClient, sheet),
      freshPeople(queryClient, sheet),
      freshClaims(queryClient, sheet),
      freshRates(queryClient, sheet),
      freshPeriods(queryClient, sheet),
      freshSettings(queryClient, sheet),
    ]);
    const rules = dueRules(settings, info.today, claims);
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people) {
      const own = claims.filter((c) => c.personId === p.id).reverse();
      for (const c of own) {
        lines.push([
          p.name,
          c.number ?? '',
          c.date,
          c.amount,
          claimDue(c, rates, p.grades, rules),
          c.status,
          periodOf(periods, c.dueBy)?.name ?? '',
        ]);
      }
    }
    const csv = lines.map((l) => l.map(csvField).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `claims-${sheet}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <button type="button" data-testid="export-claims" onClick={download}>
      Export claims (CSV)
    </button>
  );
}

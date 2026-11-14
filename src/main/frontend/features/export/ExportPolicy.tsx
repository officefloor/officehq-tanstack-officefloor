import { useQueryClient } from '@tanstack/react-query';
import { counts } from '../claims/claims';
import { liveHolds } from '../people/people';
import { policyVersionOn } from '../policy/policy';
import { claimDue, total } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { dueRules } from '../settings/settings';
import { csvField } from './ExportPeople';
import { freshClaims, freshPeople, freshRates, freshSettings, freshSheetInfo } from './fresh';

// The button that downloads the policy versions as a CSV file: oldest first, each its number, its from-date (empty
// when it has none), the setting and value it set, how many claims it is the policy version of (the one in force on
// the claim's date, as the policy versions list counts them) and the dues of those of them that count toward due-total,
// added up (skipping n/a). Quoted and ended as the people export; re-read from the store first, as the people export is.
export function ExportPolicy() {
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
    const rules = dueRules(settings, info.today, claims);
    const versions = settings.policyVersions;
    const listed = claims.filter((c) => c.status !== 'DELETED');
    const lines = [['version', 'from', 'key', 'value', 'claims', 'due-total']];
    for (const v of versions) {
      const own = listed.filter((c) => policyVersionOn(versions, c.date) === v.number);
      const dues = own.flatMap((c) => {
        const p = people.find((x) => x.id === c.personId);
        if (!p || !counts(c, liveHolds(p, rules.today), rules.today)) return [];
        return [claimDue(c, rates, p.grades, rules)];
      });
      lines.push([String(v.number), v.from, v.key, v.value, String(own.length), total(dues)]);
    }
    const csv = lines.map((l) => l.map(csvField).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `policy-${sheet}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <button type="button" data-testid="export-policy" onClick={download}>
      Export policy versions (CSV)
    </button>
  );
}

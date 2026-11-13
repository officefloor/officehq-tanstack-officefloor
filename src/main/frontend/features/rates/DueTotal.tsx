import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';
import { personDue, trimmedTotal, useRates } from './rates';

// The sheet's due-total: everyone's due-total added up, skipping those that are n/a, trimmed of the highest and lowest.
export function DueTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  if (!user || !sheet) return null;

  const dues = people.map((p) => personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, p.holds, rules));
  return (
    <p>
      Due total: <strong data-testid="rollup-due-total">{trimmedTotal(dues)}</strong>
    </p>
  );
}

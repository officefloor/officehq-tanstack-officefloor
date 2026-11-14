import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { personSplit, total, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';

// The sheet's settled-total and open-total: everyone's added up (not trimmed).
export function SplitTotals() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  if (!user || !sheet) return null;

  const each = people.map((p) =>
    personSplit(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules, {
      personId: p.id,
      claims,
      people,
    }),
  );
  return (
    <p>
      Settled total: <strong data-testid="rollup-settled-total">{total(each.map((s) => s.settled))}</strong>, open
      total: <strong data-testid="rollup-open-total">{total(each.map((s) => s.open))}</strong>
    </p>
  );
}

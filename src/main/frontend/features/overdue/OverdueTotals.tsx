import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { trimmedTotal, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';
import { personOverdue } from './overdue';

// The sheet's overdue count and overdue-total: everyone's added up (the total trimmed of the highest and lowest).
export function OverdueTotals() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  if (!user || !sheet) return null;

  const each = people.map((p) =>
    personOverdue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules),
  );
  return (
    <p>
      Overdue: <strong data-testid="rollup-overdue-count">{each.reduce((n, o) => n + o.count, 0)}</strong>, totalling{' '}
      <strong data-testid="rollup-overdue-total">{trimmedTotal(each.map((o) => o.total))}</strong>
    </p>
  );
}

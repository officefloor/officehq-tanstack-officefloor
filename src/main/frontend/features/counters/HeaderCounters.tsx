import { useClaims } from '../claims/claims';
import { personOverdue } from '../overdue/overdue';
import { usePeople } from '../people/people';
import { personDue, trimmedTotal, useRates } from '../rates/rates';
import { useDueRules } from '../settings/settings';

/** The statuses counted, as in the summary; one no claim reaches yet still shows, as 0. */
const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

// The sheet's counters, always in the header: everyone on it (retired people included), its claims per
// status, and its due-total and overdue-total — each worked out exactly as the figure it repeats.
export function HeaderCounters() {
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  const mine = (id: number) => claims.filter((c) => c.personId === id);
  const dues = people.map((p) => personDue(mine(p.id), rates, p.grades, p.holds, rules));
  const overdue = people.map((p) => personOverdue(mine(p.id), rates, p.grades, p.holds, rules).total);
  return (
    <p data-testid="header-counters">
      People: <strong data-testid="header-people-count">{people.length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s}: <strong data-testid={`header-claims-${s}-count`}>{claims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}
      {' · '}Due: <strong data-testid="header-due-total">{trimmedTotal(dues)}</strong>
      {' · '}Overdue: <strong data-testid="header-overdue-total">{trimmedTotal(overdue)}</strong>
    </p>
  );
}

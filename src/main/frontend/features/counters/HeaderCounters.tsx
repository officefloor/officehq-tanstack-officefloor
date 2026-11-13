import { useClaims } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { personOverdue } from '../overdue/overdue';
import { liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personDue, trimmedTotal, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { useDueRules } from '../settings/settings';
import type { DueRules } from '../settings/settings';

/** The statuses counted, as in the summary; one no claim reaches yet still shows, as 0. */
const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

/** A sheet's due-total and overdue-total as its header shows them. */
export function sheetTotals(people: Person[], claims: Claim[], rates: Rate[], rules: DueRules): { due: string; overdue: string } {
  const mine = (id: number) => claims.filter((c) => c.personId === id);
  const dues = people.map((p) => personDue(mine(p.id), rates, p.grades, liveHolds(p, rules.today), rules));
  const overdue = people.map((p) => personOverdue(mine(p.id), rates, p.grades, liveHolds(p, rules.today), rules).total);
  return { due: trimmedTotal(dues), overdue: trimmedTotal(overdue) };
}

// The sheet's counters, always in the header: everyone on it (retired people included), its claims per
// status, and its due-total and overdue-total — each worked out exactly as the figure it repeats.
export function HeaderCounters() {
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  const totals = sheetTotals(people, claims, rates, rules);
  return (
    <p data-testid="header-counters">
      People: <strong data-testid="header-people-count">{people.length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s}: <strong data-testid={`header-claims-${s}-count`}>{claims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}
      {' · '}Due: <strong data-testid="header-due-total">{totals.due}</strong>
      {' · '}Overdue: <strong data-testid="header-overdue-total">{totals.overdue}</strong>
    </p>
  );
}

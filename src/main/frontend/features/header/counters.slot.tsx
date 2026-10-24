import { HomeMain } from '../../slots/defs/homeMain';
import { useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { overdueOf } from '../claims/overdue.slot';
import { personDue } from '../rates/due.slot';
import { trimmedSum, useRates, type Rate } from '../rates/rates';
import { useSettings, type Settings } from '../roles/roles';
import type { Person } from '../people/people';
import type { Claim } from '../claims/claims';
import { STATUSES } from '../summary/summary.slot';

/** A sheet's due-total and overdue-total, as the rollup figures them, from what the store holds for it. */
export function sheetTotals(people: Person[], claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): { due: string; overdue: string } {
  return {
    due: trimmedSum(people.map((p) => personDue(p, claims, rates, today, settings))),
    overdue: trimmedSum(people.map((p) => overdueOf(p, claims, rates, today, settings).total)),
  };
}

// The header counters, shown at all times: how many people the sheet has (retired ones included), how many of its
// claims have each status (as the summary counts them), and the sheet's due-total and overdue-total (as the rollup
// figures them).
function HeaderCounters() {
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const today = useSheetToday();
  const allPeople = people.data ?? [];
  const allClaims = claims.data ?? [];
  const { due, overdue } = sheetTotals(allPeople, allClaims, rates.data, today, settings.data);
  return (
    <section data-testid="header-counters">
      People <strong data-testid="header-people-count">{allPeople.length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}{s} <strong data-testid={`header-claims-${s}-count`}>{allClaims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}
      {' · '}Due <strong data-testid="header-due-total">{due}</strong>
      {' · '}Overdue <strong data-testid="header-overdue-total">{overdue}</strong>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 1, Component: HeaderCounters });

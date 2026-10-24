import { HomeMain } from '../../slots/defs/homeMain';
import { useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { overdueOf } from '../claims/overdue.slot';
import { personDue } from '../rates/due.slot';
import { sumDue, useRates } from '../rates/rates';
import { useSettings } from '../roles/roles';
import { STATUSES } from '../summary/summary.slot';

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
  const due = sumDue(allPeople.map((p) => personDue(p, allClaims, rates.data, today, settings.data)));
  const overdue = sumDue(allPeople.map((p) => overdueOf(p, allClaims, rates.data, today, settings.data).total));
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

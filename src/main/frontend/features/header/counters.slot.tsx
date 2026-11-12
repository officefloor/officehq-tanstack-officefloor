import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet } from '../../session/session';
import { STATUSES, statusCount, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useSheetDueTotal } from '../rates/due.slot';
import { useSheetOverdue } from '../overdue/overdue.slot';

// The sheet's counters, always in the header: its people (retired people included), its claims per
// status (the summary's counts) and its due- and overdue-totals (the rollup's), from the same figures.
function HeaderCounters() {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const due = useSheetDueTotal();
  const overdue = useSheetOverdue();

  return (
    <p data-testid="header-counters">
      People <strong data-testid="header-people-count">{(people.data ?? []).length}</strong>{' '}
      {STATUSES.map((s) => (
        <span key={s}>
          {s} <strong data-testid={`header-claims-${s}-count`}>{statusCount(claims.data, s)}</strong>{' '}
        </span>
      ))}
      Due total <strong data-testid="header-due-total">{due}</strong>{' '}
      Overdue total <strong data-testid="header-overdue-total">{overdue.total}</strong>
    </p>
  );
}

export const contribution = AppHeader.fill({ order: 30, Component: HeaderCounters });

import { SheetBody } from '../../slots/defs/sheetBody';
import { useDelegations, useRoles } from '../access/useAccess';
import { useClaims } from '../claims/useClaims';
import { useOverdue } from '../overdue/overdue.slot';
import { useSheetToday } from '../today/today.slot';

// The steward's dashboard: the sheet's PENDING claims, its REVIEW claims and its overdue claims that count (as the
// overdue rollup counts them). Shown to a steward, or one a steward's delegation holds on the sheet's today; to anyone
// else the three cells are blank. Under an as-of date the roles, delegations and claims are as they stood then.
function StewardDashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const roles = useRoles(sheetId).data ?? [];
  const delegations = useDelegations(sheetId).data ?? [];
  const today = useSheetToday(sheetId).data?.today;
  const claims = useClaims(sheetId).data ?? [];
  const overdue = useOverdue(sheetId).sheet;
  const steward = (name: string) => roles.some((r) => r.user === name && r.role === 'steward');
  const acting = steward(user) || (today !== undefined && delegations.some((d) =>
    d.user === user && steward(d.by) && d.from <= today && today <= d.to));
  const count = (status: string) => (acting ? String(claims.filter((c) => c.status === status).length) : '');
  return (
    <section data-testid="steward-dashboard">
      <p>
        Pending: <strong data-testid="dash-pending-count">{count('PENDING')}</strong>{' '}
        Review: <strong data-testid="dash-review-count">{count('REVIEW')}</strong>{' '}
        Overdue: <strong data-testid="dash-overdue-count">{acting ? String(overdue.count) : ''}</strong>
      </p>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -9, Component: StewardDashboard });

import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { statusCount, useClaims } from '../claims/claims';
import { useSheetOverdue } from '../overdue/overdue.slot';
import { useSheetToday } from '../sheet/sheet';
import { useDelegations, useRoles } from './roles';

/** Whether the user acts as a steward on the sheet's today (the as-of date under one): steward by role
 *  as it stood then, or by a delegation then in force. */
function useIsSteward(sheet: string, user: string): boolean {
  const roles = useRoles(sheet).data;
  const delegations = useDelegations(sheet).data;
  const today = useSheetToday(sheet);
  const name = user.trim();
  if (name === '' || roles === undefined || delegations === undefined || today === undefined) return false;
  if (roles.some((r) => r.user === name && r.role === 'steward')) return true;
  return delegations.some((d) => d.user === name && d.from <= today && today <= d.to);
}

// A steward's dashboard: the sheet's PENDING and REVIEW claims, and its overdue claims that count. A user
// who is not a steward (as the screen's as-of date had them) sees the cells empty.
function StewardDashboard() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const steward = useIsSteward(sheet, user);
  const claims = useClaims(sheet);
  const overdue = useSheetOverdue();

  return (
    <p data-testid="steward-dashboard">
      Pending <strong data-testid="dash-pending-count">{steward ? statusCount(claims.data, 'PENDING') : ''}</strong>{' '}
      Review <strong data-testid="dash-review-count">{steward ? statusCount(claims.data, 'REVIEW') : ''}</strong>{' '}
      Overdue <strong data-testid="dash-overdue-count">{steward ? overdue.count : ''}</strong>
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 37, Component: StewardDashboard });

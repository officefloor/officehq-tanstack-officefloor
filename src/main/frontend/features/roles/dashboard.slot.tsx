import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { statusCount, useClaims } from '../claims/claims';
import { useSheetOverdue } from '../overdue/overdue.slot';
import { useDescendants, useSheetToday } from '../sheet/sheet';
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

type Counts = { pending: number; review: number; overdue: number };

/** Adds the first sheet's counts (as that sheet shows them) to those so far, then the rest's, one sheet per
 *  component so each reads its sheet with the same hooks; shows the sums once every sheet is added. */
function DashCounts({ sheets, sofar }: { sheets: string[]; sofar: Counts }) {
  const [first, ...rest] = sheets;
  const claims = useClaims(first);
  const overdue = useSheetOverdue(first);
  const counts: Counts = {
    pending: sofar.pending + statusCount(claims.data, 'PENDING'),
    review: sofar.review + statusCount(claims.data, 'REVIEW'),
    overdue: sofar.overdue + overdue.count,
  };
  if (rest.length > 0) return <DashCounts sheets={rest} sofar={counts} />;
  return (
    <>
      Pending <strong data-testid="dash-pending-count">{counts.pending}</strong>{' '}
      Review <strong data-testid="dash-review-count">{counts.review}</strong>{' '}
      Overdue <strong data-testid="dash-overdue-count">{counts.overdue}</strong>
    </>
  );
}

// A steward's dashboard: the PENDING and REVIEW claims, and the overdue claims that count, over the sheet and
// all its descendants (each sheet read as it shows itself). A user who is not a steward of the sheet (as the
// screen's as-of date had them) sees the cells empty.
function StewardDashboard() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const steward = useIsSteward(sheet, user);
  const descendants = useDescendants(sheet);

  return (
    <p data-testid="steward-dashboard">
      {steward && descendants.data ? (
        <DashCounts key={descendants.data.join(',')} sheets={[sheet, ...descendants.data]} sofar={{ pending: 0, review: 0, overdue: 0 }} />
      ) : (
        <>
          Pending <strong data-testid="dash-pending-count"></strong>{' '}
          Review <strong data-testid="dash-review-count"></strong>{' '}
          Overdue <strong data-testid="dash-overdue-count"></strong>
        </>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 37, Component: StewardDashboard });

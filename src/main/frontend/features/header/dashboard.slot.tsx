import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentUser, useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { overdueOf } from '../claims/overdue.slot';
import { useRates } from '../rates/rates';
import { useDelegations, useRoles, useSettings, type Delegation, type Role } from '../roles/roles';

/** The role a user name acts in on the sheet, as the server finds it: steward while the sheet's today is inside a
 *  delegation to it, else its own role (clerk when none is set). */
export function roleOf(roles: Role[], delegations: Delegation[], user: string | undefined, today: string | undefined): string {
  if (!user) return '';
  const own = roles.find((r) => r.user === user)?.role ?? 'clerk';
  const delegated = !!today && delegations.some((d) => d.user === user && d.from <= today && d.to >= today);
  return own !== 'steward' && delegated ? 'steward' : own;
}

// The steward's dashboard: over the sheet, how many claims are PENDING, how many are in REVIEW, and how many claims
// that count are overdue (as the rollup counts them). A user who is not a steward (in the role they act in on the
// sheet's today) sees the three cells blank. In an as-of view every input is as of that date, the role included.
function StewardDashboard() {
  const [user] = useCurrentUser();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const roles = useRoles();
  const delegations = useDelegations();
  const today = useSheetToday();
  const steward = roles.data !== undefined && delegations.data !== undefined
    && roleOf(roles.data, delegations.data, user, today) === 'steward';
  const allClaims = claims.data ?? [];
  const pending = steward ? String(allClaims.filter((c) => c.status === 'PENDING').length) : '';
  const review = steward ? String(allClaims.filter((c) => c.status === 'REVIEW').length) : '';
  const overdue = steward
    ? String((people.data ?? []).reduce((n, p) => n + overdueOf(p, allClaims, rates.data, today, settings.data).count, 0))
    : '';
  return (
    <section data-testid="steward-dashboard">
      Pending <strong data-testid="dash-pending-count">{pending}</strong>
      {' · '}Review <strong data-testid="dash-review-count">{review}</strong>
      {' · '}Overdue <strong data-testid="dash-overdue-count">{overdue}</strong>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 1.2, Component: StewardDashboard });

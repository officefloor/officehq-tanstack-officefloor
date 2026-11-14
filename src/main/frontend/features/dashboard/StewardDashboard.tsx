import { useClaims } from '../claims/claims';
import { personOverdue } from '../overdue/overdue';
import { liveHolds, usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { useCurrentUser } from '../session/session';
import { useDueRules, useSettings } from '../settings/settings';
import type { Settings } from '../settings/settings';

/** Whether the user acts as a steward on the date: given the role, or by a delegation in force on it. */
export function actsAsSteward(settings: Settings | undefined, user: string, today: string): boolean {
  if (!settings || !user) return false;
  return (
    settings.roles.some((r) => r.user === user && r.role === 'steward') ||
    settings.delegations.some((d) => d.user === user && d.from <= today && today <= d.to)
  );
}

// A steward's dashboard over the sheet: how many claims are PENDING, how many REVIEW, and how many overdue claims count
// (as the rollup's overdue count). Blank for a user who does not act as a steward. While the screen shows the sheet as
// of a date, everything — the role included — is as it stood then.
export function StewardDashboard() {
  const [user] = useCurrentUser();
  const settings = useSettings().data;
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  const steward = actsAsSteward(settings, user ?? '', rules.today);
  const overdue = people.reduce(
    (n, p) => n + personOverdue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules).count,
    0,
  );
  return (
    <p data-testid="steward-dashboard">
      Steward — Pending: <strong data-testid="dash-pending-count">{steward ? claims.filter((c) => c.status === 'PENDING').length : ''}</strong>
      {' · '}Review: <strong data-testid="dash-review-count">{steward ? claims.filter((c) => c.status === 'REVIEW').length : ''}</strong>
      {' · '}Overdue: <strong data-testid="dash-overdue-count">{steward ? overdue : ''}</strong>
    </p>
  );
}

import { currentAsOf } from '../../api/http';
import { useAliases } from '../aliases/aliases';
import { useClaims } from '../claims/claims';
import { sheetFigures, useStoredFigures } from '../counters/ConsolidatedFigures';
import { usePeople } from '../people/people';
import { usePools } from '../pools/pools';
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

// A steward's dashboard over the sheet and all its descendants: how many claims are PENDING, how many REVIEW, and how many overdue claims count
// (as the rollup's overdue count), each added up over the tree, each sheet by its own rules. Blank for a user who does not act as a steward. While the screen shows the sheet as
// of a date, everything — the role included — is as it stood then, over every sheet of the tree.
export function StewardDashboard() {
  const [user] = useCurrentUser();
  const settings = useSettings().data;
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const aliases = useAliases().data ?? [];
  const pools = usePools().data ?? [];
  const stored = useStoredFigures(currentAsOf()).data ?? [];

  const steward = actsAsSteward(settings, user ?? '', rules.today);
  const all = [sheetFigures(people, aliases, claims, rates, rules, pools), ...stored];
  const pending = all.reduce((n, f) => n + f.statuses.PENDING, 0);
  const review = all.reduce((n, f) => n + f.statuses.REVIEW, 0);
  const overdue = all.reduce((n, f) => n + f.overdueCount, 0);
  return (
    <p data-testid="steward-dashboard">
      Steward — Pending: <strong data-testid="dash-pending-count">{steward ? pending : ''}</strong>
      {' · '}Review: <strong data-testid="dash-review-count">{steward ? review : ''}</strong>
      {' · '}Overdue: <strong data-testid="dash-overdue-count">{steward ? overdue : ''}</strong>
    </p>
  );
}

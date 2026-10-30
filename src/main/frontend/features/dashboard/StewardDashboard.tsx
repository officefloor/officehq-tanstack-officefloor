import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSheetFigures } from '../people/useSheetFigures';
import { useSettings } from '../settings/useSettings';

// The steward's dashboard: over the sheet, how many claims are PENDING and how many REVIEW (as the
// header counts them), and how many claims that count are overdue (as the sheet's overdue-total
// adds them up). Only a steward sees the counts — the role set, or a delegation covering the sheet's
// today, as the server judges it; anyone else sees the cells empty. Under an as-of date the settings,
// claims and today are those as they stood then, so the counts and the role are the ones of then.
export function StewardDashboard() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const settings = useSettings(sheet);
  const { today, statusCount, overdue } = useSheetFigures(sheet);
  const steward =
    settings !== undefined &&
    (settings.roles.some((r) => r.user === user && r.role === 'steward') ||
      settings.delegations.some((d) => d.user === user && today !== undefined && d.from <= today && today <= d.to));
  return (
    <section data-testid="dashboard">
      Steward: pending <span data-testid="dash-pending-count">{steward ? statusCount('PENDING') : ''}</span>
      {', '}review <span data-testid="dash-review-count">{steward ? statusCount('REVIEW') : ''}</span>
      {', '}overdue <span data-testid="dash-overdue-count">{steward ? overdue.length : ''}</span>
    </section>
  );
}

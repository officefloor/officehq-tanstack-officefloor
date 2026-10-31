import { asString, useSearchParam } from '../../url/useSearchParam';
import { useDescendants } from '../consolidated/useDescendants';
import { useSheetFigures } from '../people/useSheetFigures';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';

type Counts = { pending: number; review: number; overdue: number };

// The steward's dashboard: over the sheet and all its descendants, how many claims are PENDING and
// how many REVIEW (as each sheet's header counts them), and how many claims that count are overdue
// (as each sheet's overdue-total adds them up, on its own today), added up. Only a steward of the
// sheet sees the counts — the role set, or a delegation covering the sheet's today, as the server
// judges it; anyone else sees the cells empty. Under an as-of date the settings, claims and today are
// those as they stood then, so the counts and the role are the ones of then.
export function StewardDashboard() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const settings = useSettings(sheet);
  const today = useSheetToday(sheet);
  const descendants = useDescendants(sheet);
  const steward =
    settings !== undefined &&
    (settings.roles.some((r) => r.user === user && r.role === 'steward') ||
      settings.delegations.some((d) => d.user === user && today !== undefined && d.from <= today && today <= d.to));
  return <Add sheets={[sheet, ...descendants]} counts={{ pending: 0, review: 0, overdue: 0 }} steward={steward} />;
}

// Adds the first sheet's counts and passes on to the rest: one sheet's hooks per component, so the
// number of sheets can change.
function Add({ sheets, counts, steward }: { sheets: string[]; counts: Counts; steward: boolean }) {
  if (sheets.length === 0) return <Shown counts={counts} steward={steward} />;
  return <AddSheet key={sheets[0]} sheets={sheets} counts={counts} steward={steward} />;
}

function AddSheet({ sheets, counts, steward }: { sheets: string[]; counts: Counts; steward: boolean }) {
  const { statusCount, overdue } = useSheetFigures(sheets[0]);
  return (
    <Add
      sheets={sheets.slice(1)}
      steward={steward}
      counts={{
        pending: counts.pending + statusCount('PENDING'),
        review: counts.review + statusCount('REVIEW'),
        overdue: counts.overdue + overdue.length,
      }}
    />
  );
}

function Shown({ counts, steward }: { counts: Counts; steward: boolean }) {
  return (
    <section data-testid="dashboard">
      Steward: pending <span data-testid="dash-pending-count">{steward ? counts.pending : ''}</span>
      {', '}review <span data-testid="dash-review-count">{steward ? counts.review : ''}</span>
      {', '}overdue <span data-testid="dash-overdue-count">{steward ? counts.overdue : ''}</span>
    </section>
  );
}

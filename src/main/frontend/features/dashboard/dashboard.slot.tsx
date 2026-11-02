import { useCallback, useEffect, useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { counted, overdue, statusCount, useClaims, useLate } from '../claims/claims';
import { useUserRole } from '../roles/roles';
import { useFamily } from '../consolidated/consolidated';

type Counts = { pending: number; review: number; overdue: number };

// One sheet's dashboard counts: how many claims are PENDING, how many are in REVIEW, and how many of the claims that
// count (see counted) are overdue, by that sheet's own today and late rules. Renders nothing; it reports them up.
function SheetCounts({ sheet, report }: { sheet: string; report: (sheet: string, c: Counts) => void }) {
  const { data: claims = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const late = useLate(sheet);
  const pending = statusCount(claims, 'PENDING');
  const review = statusCount(claims, 'REVIEW');
  const overdueCount = counted(claims, people).filter((c) => overdue(c, late)).length;
  useEffect(() => report(sheet, { pending, review, overdue: overdueCount }), [sheet, report, pending, review, overdueCount]);
  return null;
}

// A steward's dashboard over the sheet and all its descendants (see useFamily): each sheet's counts (see SheetCounts),
// added up. A sheet with no children shows its own. A user not acting as a steward on the sheet sees the cells empty.
// Under an as-of date every read is the sheet as it stood then, so the counts and the role are those of then.
function Dashboard({ sheet, user }: { sheet: string; user: string }) {
  const { data: family = [sheet] } = useFamily(sheet);
  const { data: role } = useUserRole(sheet, user);
  const [counts, setCounts] = useState<Map<string, Counts>>(new Map());
  const report = useCallback((s: string, c: Counts) => setCounts((prev) => new Map(prev).set(s, c)), []);
  const shown = family.map((s) => counts.get(s)).filter((c): c is Counts => c !== undefined);
  const total = (k: keyof Counts) => shown.reduce((n, c) => n + c[k], 0);
  const steward = role?.role === 'steward';

  return (
    <section>
      {family.map((s) => (
        <SheetCounts key={s} sheet={s} report={report} />
      ))}
      <h2>Dashboard</h2>
      <p>
        Pending: <strong data-testid="dash-pending-count">{steward ? total('pending') : ''}</strong>{' '}
        Review: <strong data-testid="dash-review-count">{steward ? total('review') : ''}</strong>{' '}
        Overdue: <strong data-testid="dash-overdue-count">{steward ? total('overdue') : ''}</strong>
      </p>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 29, Component: Dashboard });

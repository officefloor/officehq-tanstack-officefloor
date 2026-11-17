import { useCallback, useEffect, useState } from 'react';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useDescendants } from '../consolidated/descendants';
import { useSheetOverdue, useStatusCounts } from '../header/totals';
import { useActingRole } from '../roles/roles';

/** One sheet's own dashboard counts. */
type Counts = { pending: number; review: number; overdue: number };

/** Works out one sheet's counts with the same hooks as its header, and reports them. Shows nothing. */
function SheetCounts({ sheetId, report }: { sheetId: string; report: (sheetId: string, c: Counts) => void }) {
  const withStatus = useStatusCounts(sheetId);
  const overdue = useSheetOverdue(sheetId).count;
  const pending = withStatus('PENDING');
  const review = withStatus('REVIEW');
  useEffect(() => report(sheetId, { pending, review, overdue }), [report, sheetId, pending, review, overdue]);
  return null;
}

/** The steward's dashboard: how many claims are PENDING, how many REVIEW, and how many that count are overdue (as the
 *  rollups count them), over the sheet and all its descendants (each sheet's counts added up). Empty for a user who
 *  does not act as a steward on the sheet; as of a date, the counts as they stood then, for the role the user had
 *  then. */
function Dashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const role = useActingRole(sheetId, user);
  const descendants = useDescendants(sheetId);
  const sheets = [sheetId, ...(descendants.data ?? [])];
  const [counts, setCounts] = useState<Record<string, Counts>>({});
  const report = useCallback((id: string, c: Counts) => setCounts((all) => ({ ...all, [id]: c })), []);
  const counted = sheets.map((id) => counts[id]).filter((c): c is Counts => c !== undefined);
  const total = (k: keyof Counts) => counted.reduce((n, c) => n + c[k], 0);
  const steward = role.data?.role === 'steward';
  return (
    <section data-testid="dashboard">
      {sheets.map((id) => (
        <SheetCounts key={id} sheetId={id} report={report} />
      ))}
      Pending <span data-testid="dash-pending-count">{steward ? total('pending') : ''}</span>
      {' · '}Review <span data-testid="dash-review-count">{steward ? total('review') : ''}</span>
      {' · '}Overdue <span data-testid="dash-overdue-count">{steward ? total('overdue') : ''}</span>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: -5, Component: Dashboard });

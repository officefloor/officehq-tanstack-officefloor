import { useEffect, useState } from 'react';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { useDescendants } from '../consolidated/useDescendants';
import { roleOf, useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { useOverdue } from '../overdue/useOverdue';

/** One sheet's dashboard counts: its claims PENDING, in REVIEW, and overdue (see `useOverdue`). */
type Counts = { pending: number; review: number; late: number };

/** Works out one sheet's counts and hands them up once its claims are read; shows nothing. */
function SheetCounts({ sheetId, onCounts }: { sheetId: string; onCounts: (id: string, c: Counts) => void }) {
  const claims = useClaims(sheetId).data;
  const overdue = useOverdue(sheetId);
  const loaded = claims !== undefined;
  const pending = (claims ?? []).filter((c) => c.status === 'PENDING').length;
  const review = (claims ?? []).filter((c) => c.status === 'REVIEW').length;
  const late = [...overdue.values()].reduce((n, f) => n + f.count, 0);
  useEffect(() => {
    if (loaded) onCounts(sheetId, { pending, review, late });
  }, [sheetId, loaded, pending, review, late, onCounts]);
  return null;
}

/** A steward's dashboard: how many claims over the sheet and all its descendants are PENDING, how many are in
 *  REVIEW, and how many that count are overdue (see `useOverdue`, each sheet by its own today). Empty for a user
 *  who does not act as a steward on the sheet. Under an as-of date every read is as the sheet stood then, so the
 *  counts and the role are those of that day. */
function Dashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  const descendants = useDescendants(sheetId);
  const [counts, setCounts] = useState<Record<string, Counts>>({});
  const [onCounts] = useState(() => (id: string, c: Counts) => setCounts((all) => ({ ...all, [id]: c })));
  const sheets = [sheetId, ...(descendants.data ?? [])];
  const got = sheets.map((id) => counts[id]);
  const shown = descendants.data !== undefined && got.every((c) => c !== undefined)
    && settings !== undefined && today !== undefined && roleOf(settings, user, today) === 'steward';
  const total = (pick: (c: Counts) => number) => (shown ? String(got.reduce((n, c) => n + pick(c!), 0)) : '');
  return (
    <p data-testid="dashboard">
      {sheets.map((id) => (
        <SheetCounts key={id} sheetId={id} onCounts={onCounts} />
      ))}
      Steward dashboard: pending <strong data-testid="dash-pending-count">{total((c) => c.pending)}</strong>,
      review <strong data-testid="dash-review-count">{total((c) => c.review)}</strong>,
      overdue <strong data-testid="dash-overdue-count">{total((c) => c.late)}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({
  order: 36,
  Component: ({ sheetId, user }) => <Dashboard sheetId={sheetId} user={user} />,
});

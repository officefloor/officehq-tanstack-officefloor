import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { roleOf, useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { useOverdue } from '../overdue/useOverdue';

/** A steward's dashboard: how many claims on the sheet are PENDING, how many are in REVIEW, and how many
 *  that count are overdue (see `useOverdue`). Empty for a user who does not act as a steward. Under an
 *  as-of date every read is as the sheet stood then, so the counts and the role are those of that day. */
function Dashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const claims = useClaims(sheetId).data;
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  const overdue = useOverdue(sheetId);
  const shown = claims !== undefined && settings !== undefined && today !== undefined
    && roleOf(settings, user, today) === 'steward';
  const pending = shown ? String(claims.filter((c) => c.status === 'PENDING').length) : '';
  const review = shown ? String(claims.filter((c) => c.status === 'REVIEW').length) : '';
  const late = shown ? String([...overdue.values()].reduce((n, f) => n + f.count, 0)) : '';
  return (
    <p data-testid="dashboard">
      Steward dashboard: pending <strong data-testid="dash-pending-count">{pending}</strong>,
      review <strong data-testid="dash-review-count">{review}</strong>,
      overdue <strong data-testid="dash-overdue-count">{late}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({
  order: 36,
  Component: ({ sheetId, user }) => <Dashboard sheetId={sheetId} user={user} />,
});

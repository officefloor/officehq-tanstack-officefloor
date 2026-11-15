import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims } from '../claims/api';
import { useOverdue } from '../rates/Overdue';
import { roleOf, roleOfKey } from '../settings/api';
import { statusCount } from './Summary';

/** The steward's dashboard: how many of the sheet's claims are PENDING, how many REVIEW, and how many
 *  of those that count are overdue (as the overdue line counts them). Blank for anyone not acting as a
 *  steward; shown as of a date, the counts and the role are as they stood then. */
export function Dashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const role = useQuery({ queryKey: roleOfKey(sheetId, user), queryFn: () => roleOf(sheetId, user) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const late = useOverdue(sheetId, user);
  const steward = role.data?.role === 'steward' && claims.data !== undefined;
  return (
    <p>
      Steward's dashboard: pending <span data-testid="dash-pending-count">{steward ? statusCount(claims.data, 'PENDING') : ''}</span>
      {' · '}review <span data-testid="dash-review-count">{steward ? statusCount(claims.data, 'REVIEW') : ''}</span>
      {' · '}overdue <span data-testid="dash-overdue-count">{steward ? late.count : ''}</span>
    </p>
  );
}

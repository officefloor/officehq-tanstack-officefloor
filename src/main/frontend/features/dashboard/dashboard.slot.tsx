import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { overdueDues, useRates } from '../rates/rates';
import type { Role } from '../roles/roles.slot';
import { rolesKey } from '../roles/roles.slot';
import { useFloor, useLate } from '../settings/settings';
import { useAsOf } from '../sheet/sheet';

/** The role the user acts in on the sheet (a delegation in force makes them a steward). Keyed under the
 *  sheet's roles, so setting a role re-reads it. */
function useActingRole(sheetId: string, user: string) {
  return useQuery({
    queryKey: [...rolesKey(sheetId), user] as const,
    queryFn: () =>
      getJson<Role>(`/api/sheets/${encodeURIComponent(sheetId)}/roles/${encodeURIComponent(user)}`),
  }).data?.role;
}

/** The steward's dashboard: over the sheet, how many claims are PENDING, how many REVIEW, and how many
 *  overdue claims count (as the overdue rollup counts them). Anyone not acting as a steward sees the cells empty,
 *  and so does everyone while the screen shows the sheet as of a date. */
function Dashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const [asOf] = useAsOf();
  const steward = useActingRole(sheetId, user) === 'steward' && !asOf;
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const show = (n: number) => (steward ? String(n) : '');
  return (
    <section data-testid="dashboard">
      <h2>Steward's dashboard</h2>
      <table>
        <tbody>
          <tr>
            <th>Pending</th>
            <td data-testid="dash-pending-count">{show(claims.filter((c) => c.status === 'PENDING').length)}</td>
          </tr>
          <tr>
            <th>Review</th>
            <td data-testid="dash-review-count">{show(claims.filter((c) => c.status === 'REVIEW').length)}</td>
          </tr>
          <tr>
            <th>Overdue</th>
            <td data-testid="dash-overdue-count">{show(overdueDues(claims, people, rates, late, floor).length)}</td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 21, Component: Dashboard });

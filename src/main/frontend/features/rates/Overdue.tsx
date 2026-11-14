import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey, type Person } from '../people/api';
import { useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { claimDue, listRates, overdue, ratesKey } from './api';

/** The sheet's overdue claims: how many of the claims that count are OPEN past their due-by, and
 *  their due (with its late charge) added up. */
export function useOverdue(sheetId: string, user: string): ReturnType<typeof overdue> {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const charges = useLate(sheetId, today);
  const byId = new Map<number, Person>((people.data ?? []).map((p) => [p.id, p]));
  const live = liveClaims(claims.data ?? [], people.data).filter((c) => byId.has(c.personId));
  return overdue(live, today, (c) => claimDue(c, rates.data, (d) => gradeOn(byId.get(c.personId)!, d), floor, charges));
}

export function Overdue({ sheetId, user }: { sheetId: string; user: string }) {
  const late = useOverdue(sheetId, user);
  return (
    <p>
      Overdue <span data-testid="rollup-overdue-count">{late.count}</span>{' '}
      totalling <span data-testid="rollup-overdue-total">{late.total}</span>
    </p>
  );
}

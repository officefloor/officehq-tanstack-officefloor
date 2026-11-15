import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey, type Person } from '../people/api';
import { useFloor, useLate } from '../settings/api';
import { liveKey, useSheetToday } from '../sheet/api';
import { claimDue, listRates, overdue, ratesKey, trimmedSum } from './api';
import { Fig } from '../session/Fig';

/** The sheet's overdue claims: how many of the claims that count are OPEN past their due-by, and
 *  their due (with its late charge) added up per person, the sheet's total leaving out the single
 *  highest and lowest person's when there are three or more people (`current`: as it is now, even while
 *  shown as of a date). */
export function useOverdue(sheetId: string, user: string, current = false): ReturnType<typeof overdue> {
  const people = useQuery({ queryKey: liveKey(peopleKey(sheetId), current), queryFn: () => listPeople(sheetId, current) });
  const claims = useQuery({ queryKey: liveKey(claimsKey(sheetId), current), queryFn: () => listClaims(sheetId, current) });
  const rates = useQuery({ queryKey: liveKey(ratesKey(sheetId), current), queryFn: () => listRates(sheetId, current) });
  const floor = useFloor(sheetId, current);
  const today = useSheetToday(sheetId, user, current);
  const charges = useLate(sheetId, today, current);
  const byId = new Map<number, Person>((people.data ?? []).map((p) => [p.id, p]));
  const live = liveClaims(claims.data ?? [], people.data).filter((c) => byId.has(c.personId));
  const dueOf = (c: Parameters<typeof claimDue>[0]) => claimDue(c, rates.data, (d) => gradeOn(byId.get(c.personId)!, d), floor, charges);
  const perPerson = (people.data ?? []).map((p) => overdue(live.filter((c) => c.personId === p.id), charges, dueOf).total);
  return { count: overdue(live, charges, dueOf).count, total: trimmedSum(perPerson) };
}

export function Overdue({ sheetId, user }: { sheetId: string; user: string }) {
  const late = useOverdue(sheetId, user);
  return (
    <p>
      Overdue <span data-testid="rollup-overdue-count">{late.count}</span>{' '}
      totalling <span data-testid="rollup-overdue-total"><Fig>{late.total}</Fig></span>
    </p>
  );
}

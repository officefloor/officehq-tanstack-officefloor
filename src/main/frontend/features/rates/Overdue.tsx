import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { useFloor, useTerms } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { claimDue, listRates, overdue, ratesKey } from './api';

/** The sheet's overdue claims: how many of the claims that count are OPEN past their due-by, and
 *  their due added up. */
export function Overdue({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const terms = useTerms(sheetId);
  const today = useSheetToday(sheetId, user);
  const grades = new Map((people.data ?? []).map((p) => [p.id, p.grade]));
  const live = liveClaims(claims.data ?? [], people.data).filter((c) => grades.has(c.personId));
  const late = overdue(live, terms, today, (c) => claimDue(c, rates.data, grades.get(c.personId) ?? '', floor));
  return (
    <p>
      Overdue <span data-testid="rollup-overdue-count">{late.count}</span>{' '}
      totalling <span data-testid="rollup-overdue-total">{late.total}</span>
    </p>
  );
}

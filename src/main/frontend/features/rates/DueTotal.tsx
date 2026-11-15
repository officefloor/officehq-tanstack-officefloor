import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { listRates, personDue, rateByGrade, ratesKey, trimmedSum } from './api';

/** The sheet's due-total: everyone's due-total added up, skipping those shown n/a, less the single
 *  highest and lowest when three or more have one. */
export function useDueTotal(sheetId: string, user: string): string {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  const byGrade = rateByGrade(rates.data, today);
  const live = liveClaims(claims.data ?? [], people.data);
  const dues = (people.data ?? []).map((p) =>
    personDue(live.filter((c) => c.personId === p.id), rates.data, (d) => gradeOn(p, d), byGrade.get(p.grade), floor, charges, cap),
  );
  return trimmedSum(dues);
}

export function DueTotal({ sheetId, user }: { sheetId: string; user: string }) {
  return (
    <p>
      Due total <span data-testid="rollup-due-total">{useDueTotal(sheetId, user)}</span>
    </p>
  );
}

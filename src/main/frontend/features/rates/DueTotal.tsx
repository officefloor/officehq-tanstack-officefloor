import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { claimDue, listRates, personDue, rateByGrade, ratesKey, splitDues, sumDues, trimmedSum } from './api';

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

/** The sheet's settled-total and open-total: everyone's (see `splitDues`) simply added up, not trimmed. */
function useSplitTotals(sheetId: string, user: string) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const charges = useLate(sheetId, today);
  const live = liveClaims(claims.data ?? [], people.data);
  const splits = (people.data ?? []).map((p) =>
    splitDues(live.filter((c) => c.personId === p.id), (c) => claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges)),
  );
  return { settled: sumDues(splits.map((s) => s.settled)), open: sumDues(splits.map((s) => s.open)) };
}

export function DueTotal({ sheetId, user }: { sheetId: string; user: string }) {
  const split = useSplitTotals(sheetId, user);
  return (
    <p>
      Due total <span data-testid="rollup-due-total">{useDueTotal(sheetId, user)}</span>{' '}
      settled <span data-testid="rollup-settled-total">{split.settled}</span>{' '}
      open <span data-testid="rollup-open-total">{split.open}</span>
    </p>
  );
}

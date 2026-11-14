import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, sumAmounts } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { useSheetToday } from '../sheet/api';
import { dueOf, listRates, rateByGrade, ratesKey, sumDues } from './api';

/** The sheet's due-total: everyone's due-total added up, skipping those with no rate,
 *  each at their grade's rate in force on the sheet's today. */
export function DueTotal({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data, useSheetToday(sheetId, user));
  const dues = (people.data ?? []).map((p) =>
    dueOf(sumAmounts((claims.data ?? []).filter((c) => c.personId === p.id)), byGrade.get(p.grade)),
  );
  return (
    <p>
      Due total <span data-testid="rollup-due-total">{sumDues(dues)}</span>
    </p>
  );
}

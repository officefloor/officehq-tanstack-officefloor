import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, sumAmounts } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { dueOf, listRates, rateByGrade, ratesKey, sumDues } from './api';

/** The sheet's due-total: everyone's due-total added up, skipping those with no rate. */
export function DueTotal({ sheetId }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data);
  const dues = (people.data ?? []).map((p) =>
    dueOf(sumAmounts((claims.data ?? []).filter((c) => c.personId === p.id)), byGrade.get(p.grade)),
  );
  return (
    <p>
      Due total <span data-testid="rollup-due-total">{sumDues(dues)}</span>
    </p>
  );
}

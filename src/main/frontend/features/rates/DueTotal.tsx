import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { useSheetToday } from '../sheet/api';
import { listRates, personDue, rateByGrade, ratesKey, sumDues } from './api';

/** The sheet's due-total: everyone's due-total added up, skipping those shown n/a. */
export function DueTotal({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data, useSheetToday(sheetId, user));
  const live = liveClaims(claims.data ?? [], people.data);
  const dues = (people.data ?? []).map((p) =>
    personDue(live.filter((c) => c.personId === p.id), rates.data, p.grade, byGrade.get(p.grade)),
  );
  return (
    <p>
      Due total <span data-testid="rollup-due-total">{sumDues(dues)}</span>
    </p>
  );
}

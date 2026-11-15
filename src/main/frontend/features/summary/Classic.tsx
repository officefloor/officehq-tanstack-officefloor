import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { classicPeriodDueTotal, listPeriods, periodsKey } from '../periods/api';
import { listRates, personDue, rateByGrade, ratesKey, sumDues } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';

/** The classic summary, shown beside the summary: per period by from-date, its due-total counting each claim by
 *  its status now (see `classicPeriodDueTotal`); and the sheet's due-total, everyone's due-total added up with
 *  no trimming. */
export function Classic({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  const byGrade = rateByGrade(rates.data, today);
  const all = claims.data ?? [];
  const live = liveClaims(all, people.data);
  const dueTotal = sumDues((people.data ?? []).map((p) =>
    personDue(live.filter((c) => c.personId === p.id), rates.data, (d) => gradeOn(p, d), byGrade.get(p.grade), floor, charges, cap)));
  const byStart = [...(periods.data ?? [])].sort((a, b) => a.from.localeCompare(b.from));

  return (
    <section>
      <h3>Classic summary</h3>
      <table>
        <tbody>
          {byStart.map((p) => (
            <tr key={p.name}>
              <td>Period {p.name}</td>
              <td>Due <span data-testid={`classic-period-${p.name}-due-total`}>
                {classicPeriodDueTotal(p, all, people.data, rates.data, floor, charges)}</span></td>
            </tr>
          ))}
          <tr>
            <td>Sheet</td>
            <td>Due <span data-testid="classic-due-total">{dueTotal}</span></td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}

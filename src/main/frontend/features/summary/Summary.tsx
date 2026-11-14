import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims, type Claim } from '../claims/api';
import { GRADES, gradeOn, listPeople, peopleKey } from '../people/api';
import { listPeriods, periodDueTotal, periodsKey } from '../periods/api';
import { listRates, personDue, rateByGrade, ratesKey, sumDues } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';

/** Every status a claim's count is shown for. */
export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

/** How many of the sheet's claims have the status. */
export function statusCount(claims: Claim[] | undefined, status: (typeof STATUSES)[number]): number {
  return (claims ?? []).filter((c) => c.status === status).length;
}

/** The sheet's summary, agreeing with the lists: per grade, how many people not retired are on it
 *  today and their due-totals added up (n/a ones skipped); per period, its due-total; per status,
 *  how many claims have it. */
export function Summary({ sheetId, user }: { sheetId: string; user: string }) {
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
  const active = (people.data ?? []).filter((p) => !p.retired);
  const dueOf = (p: (typeof active)[number]) => personDue(liveClaims(all.filter((c) => c.personId === p.id), [p]),
    rates.data, (d) => gradeOn(p, d), byGrade.get(p.grade), floor, charges, cap);

  return (
    <section>
      <h3>Summary</h3>
      <table>
        <tbody>
          {GRADES.map((g) => {
            const on = active.filter((p) => p.grade === g);
            return (
              <tr key={g}>
                <td>Grade {g}</td>
                <td>People <span data-testid={`summary-grade-${g}-count`}>{on.length}</span></td>
                <td>Due <span data-testid={`summary-grade-${g}-due-total`}>{sumDues(on.map(dueOf))}</span></td>
              </tr>
            );
          })}
          {(periods.data ?? []).map((p) => (
            <tr key={p.name}>
              <td>Period {p.name}</td>
              <td />
              <td>Due <span data-testid={`summary-period-${p.name}-due-total`}>
                {periodDueTotal(p, all, people.data, rates.data, floor, charges)}</span></td>
            </tr>
          ))}
          {STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td>Claims <span data-testid={`summary-status-${s}-count`}>{statusCount(all, s)}</span></td>
              <td />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

import { useQuery } from '@tanstack/react-query';
import { claimsKey, isActive, listClaims, listVoidReasons, liveClaims, voidReasonsKey, type Claim } from '../claims/api';
import { GRADES, gradeOn, listPeople, peopleKey } from '../people/api';
import { listPeriods, periodDueTotal, periodsKey } from '../periods/api';
import { listRates, personDue, rateByGrade, ratesKey, sumDues } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { Fig } from '../session/Fig';

/** Every status a claim's count is shown for. */
export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'] as const;

/** How many of the sheet's claims have the status. */
export function statusCount(claims: Claim[] | undefined, status: (typeof STATUSES)[number]): number {
  return (claims ?? []).filter((c) => c.status === status).length;
}

/** A period's due-total minus the previous one's, two decimals, a minus sign when lower. */
export function dueChange(previous: string, current: string): string {
  const cents = Math.round(Number(current) * 100) - Math.round(Number(previous) * 100);
  return (cents < 0 ? '-' : '') + (Math.abs(cents) / 100).toFixed(2);
}

/** The sheet's summary, agreeing with the lists: per grade, how many people not retired are on it
 *  today, their due-totals added up (n/a ones skipped) and how many of them are active (see `isActive`); per period by from-date, its due-total and its change from the period before (none for the earliest); per status,
 *  how many claims have it; per void reason on the list, how many VOID claims were voided with it. */
export function Summary({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const voidReasons = useQuery({ queryKey: voidReasonsKey(sheetId), queryFn: () => listVoidReasons(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  const byGrade = rateByGrade(rates.data, today);
  const all = claims.data ?? [];
  const active = (people.data ?? []).filter((p) => !p.retired);
  const liveOf = (p: (typeof active)[number]) => liveClaims(all.filter((c) => c.personId === p.id), [p]);
  const dueOf = (p: (typeof active)[number]) => personDue(liveOf(p),
    rates.data, (d) => gradeOn(p, d), byGrade.get(p.grade), floor, charges, cap);
  const byStart = [...(periods.data ?? [])].sort((a, b) => a.from.localeCompare(b.from));
  const periodDues = byStart.map((p) => periodDueTotal(p, all, people.data, rates.data, floor, charges));

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
                <td>Due <span data-testid={`summary-grade-${g}-due-total`}><Fig>{sumDues(on.map(dueOf))}</Fig></span></td>
                <td>Active <span data-testid={`summary-grade-${g}-active`}>{on.filter((p) => isActive(liveOf(p), today)).length}</span></td>
              </tr>
            );
          })}
          {byStart.map((p, i) => (
            <tr key={p.name}>
              <td>Period {p.name}</td>
              <td />
              <td>Due <span data-testid={`summary-period-${p.name}-due-total`}><Fig>{periodDues[i]}</Fig></span></td>
              <td>Change <span data-testid={`summary-period-${p.name}-change`}>
                <Fig>{i === 0 ? '' : dueChange(periodDues[i - 1], periodDues[i])}</Fig></span></td>
            </tr>
          ))}
          {STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td>Claims <span data-testid={`summary-status-${s}-count`}>{statusCount(all, s)}</span></td>
              <td />
            </tr>
          ))}
          {(voidReasons.data ?? []).map((r) => (
            <tr key={r.reason}>
              <td>Voided: {r.reason}</td>
              <td>Claims <span data-testid={`summary-void-${r.reason}-count`}>
                {all.filter((c) => c.status === 'VOID' && c.voidReason === r.reason).length}</span></td>
              <td />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

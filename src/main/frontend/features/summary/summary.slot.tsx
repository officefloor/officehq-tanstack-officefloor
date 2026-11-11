import { SheetBody } from '../../slots/defs/sheetBody';
import { isPendingLate, isReviewLate, useClaims } from '../claims/claims';
import type { ClaimStatus } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { diffDue, heldParts, personDueTotal, sumDue, useRates } from '../rates/rates';
import { useCap, useFloor, useLate, usePendingSla, useReviewSla } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { periodDueTotal, usePeriods } from '../periods/periods';
import { isActive } from '../active/active';
import { useVoidReasons } from '../voidreasons/voidReasons';
import { useCutTotals } from '../pools/pools';
import { useGradeCodes } from '../grades/grades';

// Every status the summary counts.
const STATUSES: ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'];

// The sheet's summary, worked from the same figures as the lists: per grade, the people on it today
// (retired people left out), their due-totals added up and how many of them are active; per
// period, its due-total and its change from the period before it by from-date (none for the
// earliest); per status, how many claims have it; per void reason on the list, how many
// VOID claims were voided with it; and how many claims are late for review.
function Summary({ sheetId }: { sheetId: string }) {
  const GRADES = useGradeCodes(sheetId);
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);
  const reviewSla = useReviewSla(sheetId);
  const pendingSla = usePendingSla(sheetId);
  const voidReasons = useVoidReasons(sheetId).data ?? [];

  const active = people.filter((p) => !p.retired);
  const claimsOf = (p: Person) => claims.filter((c) => c.personId === p.id);
  const dueOf = (p: Person) => personDueTotal(rates, p.grades, today, claimsOf(p), liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutOf(p.id));

  return (
    <section data-testid="summary">
      <h2>Summary</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due</th>
            <th>Active</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = active.filter((p) => p.grade === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{sumDue(onGrade.map(dueOf))}</td>
                <td data-testid={`summary-grade-${g}-active`}>
                  {onGrade.filter((p) => isActive(claimsOf(p), liveHolds(p, today), today)).length}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
            <th>Change</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => {
            const due = periodDueTotal(p, people, claims, rates, floor, late);
            const previous = [...periods].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0)).filter((q) => q.from < p.from).pop();
            return (
              <tr key={p.name}>
                <td>{p.name}</td>
                <td data-testid={`summary-period-${p.name}-due-total`}>{due}</td>
                <td data-testid={`summary-period-${p.name}-change`}>
                  {previous ? diffDue(due, periodDueTotal(previous, people, claims, rates, floor, late)) : ''}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Status</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{claims.filter((c) => c.status === s).length}</td>
            </tr>
          ))}
          <tr>
            <td>REVIEW late</td>
            <td data-testid="summary-review-late-count">{claims.filter((c) => isReviewLate(c, today, reviewSla)).length}</td>
          </tr>
          <tr>
            <td>PENDING late</td>
            <td data-testid="summary-pending-late-count">{claims.filter((c) => isPendingLate(c, today, pendingSla)).length}</td>
          </tr>
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Void reason</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {voidReasons.map(({ reason }) => (
            <tr key={reason}>
              <td>{reason}</td>
              <td data-testid={`summary-void-${reason}-count`}>
                {claims.filter((c) => c.status === 'VOID' && c.voidReason === reason).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 30, Component: Summary });

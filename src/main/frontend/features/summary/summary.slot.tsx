import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, isActive, isReviewLate } from '../claims/claimsQuery';
import type { Claim, ClaimStatus } from '../claims/claimsQuery';
import { GRADES, gradeOn, peopleQuery } from '../people/peopleQuery';
import { classicPeriodDue, periodDue, periodsQuery } from '../periods/periodsQuery';
import { useCapFor } from '../pools/poolsQuery';
import { difference, personDue, ratesQuery, sumDue } from '../rates/ratesQuery';
import { useFloor, useLate, useReviewSla } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { voidReasonsQuery } from '../voidReasons/voidReasonsQuery';

export const STATUSES: ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'];

/** How many of the sheet's claims have the status. */
export const statusCount = (claims: Claim[], status: ClaimStatus) => claims.filter((c) => c.status === status).length;

/**
 * The sheet at a glance, worked out from the same figures the lists show. Per grade: the people on it
 * today (retired people excluded), their due-totals added up, skipping n/a, and how many of them are active. Per period: its
 * due-total, and its change from the period before it by from-date (none for the earliest). Per status: how many claims have it, and how many REVIEW claims are past the review SLA. Per void reason on the list: how many VOID claims were voided with it.
 */
function Summary({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const periods = useQuery(periodsQuery(sheetId)).data ?? [];
  const voidReasons = useQuery(voidReasonsQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const capFor = useCapFor(sheetId);
  const reviewSla = useReviewSla(sheetId);

  const unretired = people.filter((p) => p.retiredFrom === null);
  const byFrom = [...periods].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
  const dues = new Map(byFrom.map((p) => [p.name, periodDue(p, claims, people, rates, floor, late)]));
  const periodRows = periods.map((p) => {
    const at = byFrom.indexOf(p);
    const due = dues.get(p.name) as string;
    return { period: p, due, change: at > 0 ? difference(due, dues.get(byFrom[at - 1].name) as string) : '' };
  });

  return (
    <section data-testid="summary">
      <h3>Summary</h3>
      <table data-testid="summary-grades">
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
            const onGrade = unretired.filter((p) => gradeOn(p, today) === g);
            return (
              <tr key={g} data-testid={`summary-grade-${g}`}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>
                  {sumDue(onGrade.map((p) => personDue(p, people, claims, rates, today, floor, late, capFor(p.id))))}
                </td>
                <td data-testid={`summary-grade-${g}-active`}>
                  {onGrade.filter((p) => isActive(p.id, claims, people, today)).length}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table data-testid="summary-periods">
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
            <th>Change</th>
          </tr>
        </thead>
        <tbody>
          {periodRows.map(({ period: p, due, change }) => (
            <tr key={p.name} data-testid={`summary-period-${p.name}`}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{due}</td>
              <td data-testid={`summary-period-${p.name}-change`}>{change}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="summary-statuses">
        <thead>
          <tr>
            <th>Status</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {STATUSES.map((s) => (
            <tr key={s} data-testid={`summary-status-${s}`}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{statusCount(claims, s)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Review late{' '}
        <strong data-testid="summary-review-late-count">{claims.filter((c) => isReviewLate(c, today, reviewSla)).length}</strong>
      </p>
      <table data-testid="summary-void-reasons">
        <thead>
          <tr>
            <th>Void reason</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {voidReasons.map((r) => (
            <tr key={r.reason} data-testid={`summary-void-${r.reason}`}>
              <td>{r.reason}</td>
              <td data-testid={`summary-void-${r.reason}-count`}>{r.voided}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

/**
 * The classic summary, beside the summary: per period, its due-total counting each claim by its status now (a closed
 * period shows the figure fixed when it closed), and the sheet's due-total with no trimming (everyone's added up).
 */
function ClassicSummary({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const periods = useQuery(periodsQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const capFor = useCapFor(sheetId);

  return (
    <section data-testid="classic-summary">
      <h3>Classic summary</h3>
      <table data-testid="classic-periods">
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`classic-period-${p.name}`}>
              <td>{p.name}</td>
              <td data-testid={`classic-period-${p.name}-due-total`}>{classicPeriodDue(p, claims, people, rates, floor, late)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Due <strong data-testid="classic-due-total">{sumDue(people.map((p) => personDue(p, people, claims, rates, today, floor, late, capFor(p.id))))}</strong>
      </p>
    </section>
  );
}

export const contributions = [
  SheetPanel.fill({ order: 30, Component: Summary }),
  SheetPanel.fill({ order: 31, Component: ClassicSummary }),
];

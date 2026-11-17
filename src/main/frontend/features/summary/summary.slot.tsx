import { SheetPage } from '../../slots/defs/sheetPage';
import { isActive } from '../active/active';
import { useClaims } from '../claims/claims';
import { STATUSES, useGradeDue, useStatusCounts } from '../header/totals';
import { gradeOn, usePeople } from '../people/people';
import { useGradeCodes } from '../grades/grades';
import { periodDue, usePeriods } from '../periods/periods';
import { difference, useRates } from '../rates/rates';
import { isReviewLate } from '../overdue/late';
import { useFloor, useLate, useReviewSla } from '../settings/settings';
import { useVoidReasons } from '../voidreasons/voidreasons';

/** The sheet's summary, worked out from the same figures the lists show. Per grade: the people on it on the sheet's
 *  today (retired people excluded), their due-totals added up (skipping n/a) and how many of them are active. Per period: its due-total, and its
 *  change from the period before it by start date (blank for the earliest). Per
 *  status: how many of the sheet's claims have it, and how many REVIEW claims are review-late (see isReviewLate). Per void reason on the sheet's list: how many VOID claims were voided
 *  with it. */
function Summary({ sheetId }: { sheetId: string }) {
  const gradeCodes = useGradeCodes(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const periods = usePeriods(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const reviewSla = useReviewSla(sheetId);

  const active = (people.data ?? []).filter((p) => p.retiredFrom === null);
  const onGrade = (grade: string) => active.filter((p) => gradeOn(p, late.today) === grade);
  const gradeDue = useGradeDue(sheetId);
  const byStart = [...(periods.data ?? [])].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
  const dueOf = new Map(byStart.map((p) => [p.id, periodDue(p, claims.data, people.data, rates.data, floor, late)]));
  const changeOf = new Map(
    byStart.map((p, i) => [p.id, i === 0 ? '' : difference(dueOf.get(p.id)!, dueOf.get(byStart[i - 1].id)!)]),
  );
  const withStatus = useStatusCounts(sheetId);
  const voidReasons = useVoidReasons(sheetId);

  return (
    <section data-testid="summary">
      <h3>Summary</h3>
      <table data-testid="summary-grades">
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due total</th>
            <th>Active</th>
          </tr>
        </thead>
        <tbody>
          {gradeCodes.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`summary-grade-${g}-count`}>{onGrade(g).length}</td>
              <td data-testid={`summary-grade-${g}-due-total`}>{gradeDue(g)}</td>
              <td data-testid={`summary-grade-${g}-active`}>
                {onGrade(g).filter((p) => isActive(p, claims.data, late.today)).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="summary-periods">
        <thead>
          <tr>
            <th>Period</th>
            <th>Due total</th>
            <th>Change</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>
                {dueOf.get(p.id)}
              </td>
              <td data-testid={`summary-period-${p.name}-change`}>{changeOf.get(p.id)}</td>
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
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{withStatus(s)}</td>
            </tr>
          ))}
          <tr>
            <td>Review late</td>
            <td data-testid="summary-review-late-count">
              {(claims.data ?? []).filter((c) => isReviewLate(c, reviewSla, late.today)).length}
            </td>
          </tr>
        </tbody>
      </table>
      <table data-testid="summary-void-reasons">
        <thead>
          <tr>
            <th>Void reason</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {(voidReasons.data ?? []).map((r) => (
            <tr key={r.reason}>
              <td>{r.reason}</td>
              <td data-testid={`summary-void-${r.reason}-count`}>{r.count}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 40, Component: Summary });

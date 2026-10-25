import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, usePeople } from '../people/people';
import { isReviewLate, useClaims } from '../claims/claims';
import { personDue } from '../rates/due.slot';
import { diffDue, sumDue, useRates } from '../rates/rates';
import { useSettings } from '../roles/roles';
import { periodDue, usePeriods } from '../periods/periods';
import { isActive } from '../claims/active.slot';
import { useVoidReasons } from '../voidreasons/voidreasons';
import { usePools } from '../pools/pools';
import { useGradeCodes } from '../grades/grades';

/** The statuses the summary counts claims by. */
export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'] as const;

// The summary panel, figured the same way as the list: per grade, how many people not retired have it in force on
// the sheet's today, their due-totals added up (skipping n/a) and how many of them are active; per period (by from-date), its due-total and its change from
// the period before it (none for the earliest); per status, how
// many of the sheet's claims have it, and how many REVIEW claims are late in review; per reason on the sheet's list of void reasons, how many VOID claims were voided with it.
function Summary() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const periods = usePeriods();
  const settings = useSettings();
  const pools = usePools();
  const today = useSheetToday();
  const voidReasons = useVoidReasons();
  const gradeCodes = useGradeCodes();
  if (!user || !sheet) return null;
  const current = (people.data ?? []).filter((p) => p.retiredFrom === null);
  const allClaims = claims.data ?? [];
  const sortedPeriods = [...(periods.data ?? [])].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
  const periodDues = sortedPeriods.map((p) => periodDue(p, people.data, allClaims, rates.data, settings.data, today));
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
          {gradeCodes.map((g) => {
            const onGrade = current.filter((p) => gradeOn(p, today) === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>
                  {sumDue(onGrade.map((p) => personDue(p, allClaims, rates.data, today, settings.data, people.data, pools.data)))}
                </td>
                <td data-testid={`summary-grade-${g}-active`}>
                  {onGrade.filter((p) => isActive(p, allClaims, today)).length}
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
          {sortedPeriods.map((p, i) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodDues[i]}</td>
              <td data-testid={`summary-period-${p.name}-change`}>
                {i === 0 ? '' : diffDue(periodDues[i], periodDues[i - 1])}
              </td>
            </tr>
          ))}
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
              <td data-testid={`summary-status-${s}-count`}>{allClaims.filter((c) => c.status === s).length}</td>
            </tr>
          ))}
          <tr>
            <td>REVIEW late</td>
            <td data-testid="summary-review-late-count">
              {allClaims.filter((c) => isReviewLate(c, today, settings.data)).length}
            </td>
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
          {(voidReasons.data ?? []).map((r) => (
            <tr key={r.id}>
              <td>{r.reason}</td>
              <td data-testid={`summary-void-${r.reason}-count`}>
                {allClaims.filter((c) => c.status === 'VOID' && c.voidReason === r.reason).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 33, Component: Summary });

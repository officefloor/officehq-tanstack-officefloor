import { SheetBody } from '../../slots/defs/sheetBody';
import { useActive } from '../active/useActive';
import { useClaims } from '../claims/useClaims';
import { useGrades, usePeople } from '../people/usePeople';
import { usePeriodDueTotals } from '../periods/usePeriodDueTotals';
import { usePeriods } from '../periods/usePeriods';
import { useSettings } from '../settings/useSettings';
import { useDueTotals } from '../rates/useDueTotals';
import { hundredths, money, sumDues } from '../rates/useRates';
import { countWithStatus, STATUSES } from './statusCounts';
import { useFigure } from '../locale/useLocale';

/** The sheet's summary, worked out from the same figures the lists show: per grade, how many people
 *  have it on the sheet's today (retired people left out) and their due-totals added up (skipping
 *  'n/a') and how many of them are active; per period, its due-total and its change from the period before it by from-date (none for the earliest); per status, how many claims have it, and how many REVIEW claims are review-late; per void reason on the
 *  sheet's list, how many VOID claims were voided with it. */
function Summary({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const periods = usePeriods(sheetId);
  const dueTotals = useDueTotals(sheetId);
  const periodTotals = usePeriodDueTotals(sheetId);
  const activeById = useActive(sheetId);
  const voidReasons = useSettings(sheetId).data?.voidReasons ?? [];
  const grades = useGrades(sheetId);
  const current = (people.data ?? []).filter((p) => !p.retiredFrom);
  const byFrom = [...(periods.data ?? [])].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
  const changes = new Map<string, string>();
  byFrom.forEach((p, i) => {
    if (i === 0) return;
    const prev = periodTotals.get(byFrom[i - 1].name) ?? '0.00';
    changes.set(p.name, money(hundredths(periodTotals.get(p.name) ?? '0.00') - hundredths(prev)));
  });
  return (
    <section data-testid="summary">
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due total</th>
            <th>Active</th>
          </tr>
        </thead>
        <tbody>
          {grades.map((g) => {
            const onGrade = current.filter((p) => p.grade === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>
                  {fig(sumDues(onGrade.map((p) => dueTotals.get(p.id) ?? 'n/a')))}
                </td>
                <td data-testid={`summary-grade-${g}-active`}>
                  {onGrade.filter((p) => activeById.get(p.id)).length}
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
            <th>Due total</th>
            <th>Change</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{fig(periodTotals.get(p.name) ?? '')}</td>
              <td data-testid={`summary-period-${p.name}-change`}>{fig(changes.get(p.name) ?? '')}</td>
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
              <td data-testid={`summary-status-${s}-count`}>
                {countWithStatus(claims.data, s)}
              </td>
            </tr>
          ))}
          <tr>
            <td>Review late</td>
            <td data-testid="summary-review-late-count">{(claims.data ?? []).filter((c) => c.reviewLate).length}</td>
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
          {voidReasons.map((r) => (
            <tr key={r}>
              <td>{r}</td>
              <td data-testid={`summary-void-${r}-count`}>
                {(claims.data ?? []).filter((c) => c.status === 'VOID' && c.voidReason === r).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 40, Component: ({ sheetId }) => <Summary sheetId={sheetId} /> });

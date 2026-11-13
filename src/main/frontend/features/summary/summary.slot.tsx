import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';
import { STATUSES, isActive, live, statusCount, useClaims } from '../claims/claims';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { periodDue, usePeriods } from '../periods/periods';
import { difference, dueTotal, personDue, useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { lateOf, reviewLate } from '../overdue/overdue';
import { useVoidReasons } from '../voidreasons/voidreasons';
import { useFigure } from '../locale/locale';
import { useCapOf, useShared } from '../rates/due.slot';

// The sheet at a glance, worked out from the same figures the lists show: per grade, the people on it
// today (retired people excluded), their due-totals added up (skipping n/a) and how many of them are
// active; per period, its due-total and its change from the period before it by start date (none for
// the earliest); per status, how many claims have it; per void reason on the list,
// how many VOID claims were voided with it; and how many REVIEW claims are past the review SLA (review-late).
function Summary() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const periods = usePeriods(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  const late = lateOf(settings, today, people.data);
  const voidReasons = useVoidReasons(sheet).data ?? [];
  const shared = useShared(sheet);
  const capOf = useCapOf(sheet);

  const active = (people.data ?? []).filter((p) => !p.retired);
  const onGrade = (grade: string) => active.filter((p) => gradeOn(p.grades, today) === grade);
  const dueOf = (p: (typeof active)[number]) =>
    personDue(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), rates.data, p.grades, today, floor, late, capOf(p), shared(p));

  const periodTotal = (p: NonNullable<typeof periods.data>[number]) =>
    periodDue(p, claims.data, people.data, rates.data, floor, late);
  const byStart = [...(periods.data ?? [])].sort((a, b) => a.from.localeCompare(b.from));
  const changeOf = (p: (typeof byStart)[number]) => {
    const i = byStart.indexOf(p);
    return i > 0 ? difference(periodTotal(p), periodTotal(byStart[i - 1])) : '';
  };

  const isActiveOn = (p: (typeof active)[number]) =>
    isActive(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), today);

  return (
    <section data-testid="summary">
      <h2>Summary</h2>
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
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`summary-grade-${g}-count`}>{onGrade(g).length}</td>
              <td data-testid={`summary-grade-${g}-due-total`}>{fig(dueTotal(onGrade(g).map(dueOf)))}</td>
              <td data-testid={`summary-grade-${g}-active`}>{onGrade(g).filter(isActiveOn).length}</td>
            </tr>
          ))}
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
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>
                {fig(periodTotal(p))}
              </td>
              <td data-testid={`summary-period-${p.name}-change`}>{fig(changeOf(p))}</td>
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
              <td data-testid={`summary-status-${s}-count`}>{statusCount(claims.data, s)}</td>
            </tr>
          ))}
          <tr>
            <td>Review late</td>
            <td data-testid="summary-review-late-count">
              {(claims.data ?? []).filter((c) => reviewLate(c, today, settings?.reviewSla)).length}
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
          {voidReasons.map((r) => (
            <tr key={r.reason}>
              <td>{r.reason}</td>
              <td data-testid={`summary-void-${r.reason}-count`}>
                {(claims.data ?? []).filter((c) => c.status === 'VOID' && c.voidReason === r.reason).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 46, Component: Summary });

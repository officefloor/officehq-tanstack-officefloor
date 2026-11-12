import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';
import { STATUSES, live, statusCount, useClaims } from '../claims/claims';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { periodDue, usePeriods } from '../periods/periods';
import { dueTotal, personDue, useRates } from '../rates/rates';
import { useSettings } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { lateOf } from '../overdue/overdue';

// The sheet at a glance, worked out from the same figures the lists show: per grade, the people on it
// today (retired people excluded) and their due-totals added up (skipping n/a); per period, its
// due-total; per status, how many claims have it.
function Summary() {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const periods = usePeriods(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = settings?.floor;
  const late = lateOf(settings, today);

  const active = (people.data ?? []).filter((p) => !p.retired);
  const onGrade = (grade: string) => active.filter((p) => gradeOn(p.grades, today) === grade);
  const dueOf = (p: (typeof active)[number]) =>
    personDue(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), rates.data, p.grades, today, floor, late);

  return (
    <section data-testid="summary">
      <h2>Summary</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`summary-grade-${g}-count`}>{onGrade(g).length}</td>
              <td data-testid={`summary-grade-${g}-due-total`}>{dueTotal(onGrade(g).map(dueOf))}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>
                {periodDue(p, claims.data, people.data, rates.data, floor, late)}
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
              <td data-testid={`summary-status-${s}-count`}>{statusCount(claims.data, s)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 46, Component: Summary });

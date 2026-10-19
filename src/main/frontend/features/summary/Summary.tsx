import { useClaims } from '../claims/Claims';
import { GRADES, gradeOn, useDueTotalOf, usePeople } from '../people/People';
import { usePeriodDueTotal } from '../periods/Periods';
import { usePeriods } from '../periods/usePeriods';
import { total } from '../rates/Rates';
import { useSheetToday } from '../sheet/SheetToday';

const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

// The sheet at a glance, worked out from the same figures the lists show. Per grade: the people on
// it on the sheet's today (retired people left out) and their due-totals added up, skipping n/a. Per
// period: its due-total as its row shows it. Per status: how many of the sheet's claims have it.
export function Summary({ sheet }: { sheet: string }) {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const periods = usePeriods(sheet);
  const today = useSheetToday(sheet);
  const dueTotalOf = useDueTotalOf(sheet);
  const periodDueTotalOf = usePeriodDueTotal(sheet);
  const onGrade = (grade: string) => people.filter((p) => !p.retired && gradeOn(p, today) === grade);

  return (
    <section>
      <h2>Summary</h2>
      <table>
        <thead>
          <tr><th>Grade</th><th>People</th><th>Due total</th></tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`summary-grade-${g}-count`}>{onGrade(g).length}</td>
              <td data-testid={`summary-grade-${g}-due-total`}>{total(onGrade(g).map(dueTotalOf))}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <thead>
          <tr><th>Period</th><th>Due total</th></tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodDueTotalOf(p)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <thead>
          <tr><th>Status</th><th>Claims</th></tr>
        </thead>
        <tbody>
          {STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{claims.filter((c) => c.status === s).length}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

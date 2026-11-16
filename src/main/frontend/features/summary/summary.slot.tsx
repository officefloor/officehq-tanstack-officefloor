import { SheetPage } from '../../slots/defs/sheetPage';
import { isActive } from '../active/active';
import { live, useClaims } from '../claims/claims';
import { STATUSES, useStatusCounts } from '../header/totals';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { periodDue, usePeriods } from '../periods/periods';
import { personDue, sum, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';

/** The sheet's summary, worked out from the same figures the lists show. Per grade: the people on it on the sheet's
 *  today (retired people excluded), their due-totals added up (skipping n/a) and how many of them are active. Per period: its due-total. Per
 *  status: how many of the sheet's claims have it. */
function Summary({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const periods = usePeriods(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);

  const active = (people.data ?? []).filter((p) => p.retiredFrom === null);
  const onGrade = (grade: string) => active.filter((p) => gradeOn(p, late.today) === grade);
  const gradeDue = (grade: string) =>
    sum(
      onGrade(grade).map((p) =>
        personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p]), rates.data, p, late, floor, cap),
      ),
    );
  const withStatus = useStatusCounts(sheetId);

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
          {GRADES.map((g) => (
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
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>
                {periodDue(p, claims.data, people.data, rates.data, floor, late)}
              </td>
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
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 40, Component: Summary });

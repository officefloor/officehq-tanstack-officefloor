import { HomeMain } from '../../slots/defs/homeMain';
import { useClaims } from '../claims/claims';
import { GRADES, usePeople } from '../people/people';
import { usePeriodDue, usePeriods } from '../periods/periods';
import { show } from '../rates/rates';
import { useDueTotal } from '../rates/rates.slot';
import { useCurrentSheet } from '../session/session';

/** The statuses the summary counts claims by (a status no claim can reach counts 0). */
const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

// The sheet's summary, worked out from the same figures as the lists: per grade, the people on it today
// (retired people left out) and their due-totals added up (n/a as 0); per period, its due-total; per
// status, the count of claims in it.
function Summary() {
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const periods = usePeriods().data ?? [];
  const dueOf = useDueTotal();
  const periodDue = usePeriodDue();
  if (!sheet) return null;
  const active = people.filter((p) => !p.retiredFrom);
  return (
    <section data-testid="summary">
      <h2>Summary</h2>
      <table data-testid="summary-grades">
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = active.filter((p) => p.grade === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>
                  {show(onGrade.reduce((n, p) => n + (dueOf(p.id) ?? 0n), 0n))}
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
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{show(periodDue(p))}</td>
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
              <td data-testid={`summary-status-${s}-count`}>{claims.filter((c) => c.status === s).length}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 40, Component: Summary });

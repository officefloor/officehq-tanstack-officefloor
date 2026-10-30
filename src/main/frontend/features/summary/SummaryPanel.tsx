import { asString, useSearchParam } from '../../url/useSearchParam';
import { GRADES, gradeOn, type Person } from '../people/usePeople';
import { STATUSES } from '../people/useSheetFigures';
import { usePeriodDueTotal } from '../periods/usePeriodDueTotal';
import { usePeriods } from '../periods/usePeriods';
import { formatCents } from '../rates/due';

// The sheet at a glance, worked from the same figures the lists show. Per grade: how many people
// are on it today (retired people left out) and their due-totals added up (skipping n/a). Per
// period: its due-total, as the period list shows it. Per status: how many claims have it.
export function SummaryPanel({
  people,
  today,
  totalOf,
  statusCount,
}: {
  people: Person[];
  today: string | undefined;
  totalOf: (person: Person) => bigint | null;
  statusCount: (status: (typeof STATUSES)[number]) => number;
}) {
  const [sheet] = useSearchParam('sheet', asString);
  const periods = usePeriods(sheet);
  const periodDueTotal = usePeriodDueTotal(sheet);
  const current = people.filter((p) => !p.retired);
  return (
    <section data-testid="summary">
      <h3>Summary</h3>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = current.filter((p) => gradeOn(p, today) === g);
            const due = onGrade.reduce((sum, p) => sum + (totalOf(p) ?? 0n), 0n);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{formatCents(due)}</td>
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
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{formatCents(periodDueTotal(p))}</td>
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
              <td data-testid={`summary-status-${s}-count`}>{statusCount(s)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

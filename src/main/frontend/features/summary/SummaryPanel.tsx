import { asString, useSearchParam } from '../../url/useSearchParam';
import { GRADES, gradeOn, type Person } from '../people/usePeople';
import { STATUSES } from '../people/useSheetFigures';
import { usePeriodDueTotal } from '../periods/usePeriodDueTotal';
import { usePeriods } from '../periods/usePeriods';
import { formatCents } from '../rates/due';
import { useVoidReasons } from '../voidreasons/useVoidReasons';

// The sheet at a glance, worked from the same figures the lists show. Per grade: how many people
// are on it today (retired people left out), their due-totals added up (skipping n/a) and how many
// of them are active. Per period: its due-total, as the period list shows it, and its change from
// the period before it by start date (blank for the earliest). Per status: how many claims have it.
// Per reason on the sheet's void list: how many VOID claims were voided with it.
export function SummaryPanel({
  people,
  today,
  totalOf,
  isActive,
  statusCount,
  voidReasonCount,
}: {
  people: Person[];
  today: string | undefined;
  totalOf: (person: Person) => bigint | null;
  isActive: (person: Person) => boolean;
  statusCount: (status: (typeof STATUSES)[number]) => number;
  voidReasonCount: (reason: string) => number;
}) {
  const [sheet] = useSearchParam('sheet', asString);
  const periods = usePeriods(sheet);
  const periodDueTotal = usePeriodDueTotal(sheet);
  const voidReasons = useVoidReasons(sheet);
  const current = people.filter((p) => !p.retired);
  const byStart = [...periods].sort((a, b) => a.from.localeCompare(b.from));
  const changeOf = (p: (typeof periods)[number]) => {
    const i = byStart.indexOf(p);
    return i <= 0 ? '' : formatCents(periodDueTotal(p) - periodDueTotal(byStart[i - 1]));
  };
  return (
    <section data-testid="summary">
      <h3>Summary</h3>
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
          {GRADES.map((g) => {
            const onGrade = current.filter((p) => gradeOn(p, today) === g);
            const due = onGrade.reduce((sum, p) => sum + (totalOf(p) ?? 0n), 0n);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{formatCents(due)}</td>
                <td data-testid={`summary-grade-${g}-active`}>{onGrade.filter(isActive).length}</td>
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
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{formatCents(periodDueTotal(p))}</td>
              <td data-testid={`summary-period-${p.name}-change`}>{changeOf(p)}</td>
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
              <td data-testid={`summary-void-${r}-count`}>{voidReasonCount(r)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

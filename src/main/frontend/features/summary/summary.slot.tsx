import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { usePeople } from '../people/people.slot';
import { showPeriodDueTotal, usePeriodDueTotal, usePeriods } from '../periods/periods.slot';
import { useDueTotals } from '../rates/rates.slot';
import { showHundredths } from '../rates/useRates';

const GRADES = ['A', 'B', 'C'] as const;
export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

// The summary panel, built from the same figures as the lists so it always agrees with them. Per grade: the people
// on it today, retired people left out, and their due-totals added up (skipping those that are n/a). Per period:
// its due-total as its row shows it. Per status: the claims on the sheet with that status.
function SummaryPanel({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const totals = useDueTotals(sheetId);
  const periods = usePeriods(sheetId);
  const periodDueTotal = usePeriodDueTotal(sheetId);
  const claims = useClaims(sheetId);

  const active = (people.data ?? []).filter((p) => p.retiredFrom === null);
  return (
    <section data-testid="summary-panel">
      <h2>Summary</h2>
      <table data-testid="summary-grades">
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const on = active.filter((p) => p.grade === g);
            let sum = 0n;
            for (const p of on) sum += totals.get(p.id) ?? 0n;
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{on.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{showHundredths(sum)}</td>
              </tr>
            );
          })}
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
              <td data-testid={`summary-period-${p.name}-due-total`}>{showPeriodDueTotal(p, periodDueTotal(p))}</td>
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
              <td data-testid={`summary-status-${s}-count`}>{(claims.data ?? []).filter((c) => c.status === s).length}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 30, Component: SummaryPanel });

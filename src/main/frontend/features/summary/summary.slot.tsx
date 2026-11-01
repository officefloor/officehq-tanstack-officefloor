import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES, isRetired, usePeople } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { twoDecimals, useRates } from '../rates/rates';
import { useCapCents, useFloorCents } from '../settings/settings';
import { CLAIM_STATUSES, personDueCents, statusCount, useClaims, useLate } from '../claims/claims';
import { periodDueCents, shownDueTotal, usePeriods } from '../periods/periods';

// The sheet at a glance, figured exactly as the lists figure it. Per grade: the people on it today (retired ones
// excluded) and their due-totals added up (an n/a due-total adds nothing). Per period: its due-total, as its row shows
// it. Per status: how many of the sheet's claims have it (every claim, whoever's).
function Summary({ sheet }: { sheet: string; user: string }) {
  const { data: claims = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const cap = useCapCents(sheet);
  const late = useLate(sheet);

  const current = people.filter((p) => !isRetired(p));
  const periodTotals = periodDueCents(periods, claims, people, rates, floor, late);

  return (
    <section>
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
          {GRADES.map((g) => {
            const onGrade = current.filter((p) => p.grade === g);
            const due = onGrade.reduce((sum, p) => sum + (personDueCents(claims, rates, p, today, floor, late, cap) ?? 0n), 0n);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{twoDecimals(due)}</td>
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
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{shownDueTotal(p, periodTotals.get(p.name) ?? 0n)}</td>
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
          {CLAIM_STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{statusCount(claims, s)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 30, Component: Summary });

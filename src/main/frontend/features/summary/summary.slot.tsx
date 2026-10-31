import { SheetPanels } from '../../slots/defs/sheetPanels';
import { live, personDueCents, useClaims } from '../claims/useClaims';
import { GRADES, gradeOn, usePeople } from '../people/usePeople';
import { periodDue, usePeriods } from '../periods/usePeriods';
import { rateOn, totalDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';

const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

/**
 * The sheet in figures, each worked out as the list works it out. Per grade: how many people not
 * retired are on it on the sheet's today, and their due-totals added up (n/a ones skipped). Per
 * period: its due-total. Per status: how many of the sheet's claims have it.
 */
function SummaryPanel({ sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: everyone = [] } = usePeople(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const people = everyone.filter((p) => !p.retired);
  const dueOf = (p: (typeof people)[number]) =>
    personDueCents(
      live(all.filter((c) => c.personId === p.id), () => p.held),
      floor,
      rateOn(rates, gradeOn(p, today), today),
      (d) => rateOn(rates, gradeOn(p, d), d),
      late,
    );

  return (
    <section data-testid="summary-panel">
      <h2>Summary</h2>
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
            const on = people.filter((p) => gradeOn(p, today) === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{on.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{totalDue(on.map(dueOf))}</td>
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
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodDue(p, all, everyone, rates, floor, late)}</td>
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
              <td data-testid={`summary-status-${s}-count`}>{all.filter((c) => c.status === s).length}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 25, Component: SummaryPanel });

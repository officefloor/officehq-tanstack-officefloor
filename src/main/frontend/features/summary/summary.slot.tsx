import { SheetPanels } from '../../slots/defs/sheetPanels';
import { isActive } from '../active/active';
import { STATUSES, live, personDueCents, useClaims } from '../claims/useClaims';
import { GRADES, gradeOn, heldOn, usePeople } from '../people/usePeople';
import { periodDue, usePeriods } from '../periods/usePeriods';
import { rateOn, totalDue, useRates } from '../rates/useRates';
import { useCap, useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { useVoidReasons } from '../voidreasons/useVoidReasons';

/**
 * The sheet in figures, each worked out as the list works it out. Per grade: how many people not
 * retired are on it on the sheet's today, their due-totals added up (n/a ones skipped), and how many
 * of them are active. Per
 * period: its due-total. Per status: how many of the sheet's claims have it. Per void reason on
 * the sheet's list: how many VOID claims were voided with it.
 */
function SummaryPanel({ sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: everyone = [] } = usePeople(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const { data: rates = [] } = useRates(sheet);
  const { data: voidReasons = [] } = useVoidReasons(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const cap = useCap(sheet);
  const people = everyone.filter((p) => !p.retired);
  const dueOf = (p: (typeof people)[number]) =>
    personDueCents(
      live(all.filter((c) => c.personId === p.id), (_, date) => heldOn(p, date, today)),
      floor,
      rateOn(rates, gradeOn(p, today), today),
      (d) => rateOn(rates, gradeOn(p, d), d),
      late,
      cap,
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
            <th>Active</th>
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
                <td data-testid={`summary-grade-${g}-active`}>{on.filter((p) => isActive(p, all, today)).length}</td>
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
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodDue(p, all, everyone, rates, floor, late, today)}</td>
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
      <table>
        <thead>
          <tr>
            <th>Void reason</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {voidReasons.map((v) => (
            <tr key={v.reason}>
              <td>{v.reason}</td>
              <td data-testid={`summary-void-${v.reason}-count`}>
                {all.filter((c) => c.status === 'VOID' && c.voidReason === v.reason).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 25, Component: SummaryPanel });

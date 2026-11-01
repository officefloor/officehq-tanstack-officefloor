import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useClaims } from '../claims/useClaims';
import { useSheetTotals } from '../claims/useSheetTotals';
import { usePeople } from '../people/usePeople';
import { periodDue, usePeriods } from '../periods/usePeriods';
import { totalDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';

/**
 * The classic summary, shown beside the summary. Per period: its due-total counting each claim by
 * its status now (OPEN, SETTLED and PENDING count), not the status it had at the period's end; a
 * closed period shows the figure fixed when it closed. And the sheet's due-total with no trimming:
 * everyone's due-total added up (n/a ones skipped).
 */
function ClassicSummaryPanel({ sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: everyone = [] } = usePeople(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const { perPerson } = useSheetTotals(sheet);

  return (
    <section data-testid="classic-summary-panel">
      <h2>Classic summary</h2>
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
              <td data-testid={`classic-period-${p.name}-due-total`}>
                {p.closed && p.closedTotal !== null ? p.closedTotal : periodDue(p, all, everyone, rates, floor, late, today, true)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Due in total: <strong data-testid="classic-due-total">{totalDue(perPerson.map((p) => p.due))}</strong>
      </p>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 26, Component: ClassicSummaryPanel });

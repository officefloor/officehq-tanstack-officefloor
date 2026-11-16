import { SheetPage } from '../../slots/defs/sheetPage';
import { useClaims } from '../claims/claims';
import { useSheetDueSum } from '../header/totals';
import { usePeople } from '../people/people';
import { classicPeriodDue, usePeriods } from '../periods/periods';
import { useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';

/** The classic summary, shown beside the summary. Per period: its due-total counting each claim by its status now
 *  (OPEN, SETTLED and PENDING count; a closed period shows the figure fixed when it closed). And the sheet's
 *  due-total with no trimming: everyone's due-total added up. */
function ClassicSummary({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const periods = usePeriods(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const dueTotal = useSheetDueSum(sheetId);

  return (
    <section data-testid="classic-summary">
      <h3>Classic summary</h3>
      <table data-testid="classic-periods">
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
              <td data-testid={`classic-period-${p.name}-due-total`}>
                {classicPeriodDue(p, claims.data, people.data, rates.data, floor, late)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Due total: <span data-testid="classic-due-total">{dueTotal}</span>
      </p>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 40.5, Component: ClassicSummary });

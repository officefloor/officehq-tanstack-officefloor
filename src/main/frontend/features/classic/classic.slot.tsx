import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeriodDueTotals } from '../periods/usePeriodDueTotals';
import { usePeriods } from '../periods/usePeriods';
import { useDueTotals } from '../rates/useDueTotals';
import { sumDues } from '../rates/useRates';

/** The classic summary, shown beside the summary: per period, its due-total counting each claim by its
 *  status now (the figure fixed when it closed while it is closed); and the sheet's due-total with no
 *  trimming — everyone's due-total added up, skipping 'n/a'. */
function ClassicSummary({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId);
  const periodTotals = usePeriodDueTotals(sheetId, true);
  const dueTotals = useDueTotals(sheetId);
  return (
    <section data-testid="classic-summary">
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Classic due total</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`classic-period-${p.name}-due-total`}>{periodTotals.get(p.name) ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Classic due total: <strong data-testid="classic-due-total">{sumDues([...dueTotals.values()])}</strong>
      </p>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 41, Component: ({ sheetId }) => <ClassicSummary sheetId={sheetId} /> });

import { SheetBody } from '../../slots/defs/sheetBody';
import { useActive } from '../active/useActive';
import { useClaims } from '../claims/useClaims';
import { GRADES, usePeople } from '../people/usePeople';
import { usePeriodDueTotals } from '../periods/usePeriodDueTotals';
import { usePeriods } from '../periods/usePeriods';
import { useDueTotals } from '../rates/useDueTotals';
import { sumDues } from '../rates/useRates';
import { countWithStatus, STATUSES } from './statusCounts';

/** The sheet's summary, worked out from the same figures the lists show: per grade, how many people
 *  have it on the sheet's today (retired people left out) and their due-totals added up (skipping
 *  'n/a') and how many of them are active; per period, its due-total; per status, how many claims have it. */
function Summary({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const periods = usePeriods(sheetId);
  const dueTotals = useDueTotals(sheetId);
  const periodTotals = usePeriodDueTotals(sheetId);
  const activeById = useActive(sheetId);
  const current = (people.data ?? []).filter((p) => !p.retiredFrom);
  return (
    <section data-testid="summary">
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due total</th>
            <th>Active</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = current.filter((p) => p.grade === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>
                  {sumDues(onGrade.map((p) => dueTotals.get(p.id) ?? 'n/a'))}
                </td>
                <td data-testid={`summary-grade-${g}-active`}>
                  {onGrade.filter((p) => activeById.get(p.id)).length}
                </td>
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
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodTotals.get(p.name) ?? ''}</td>
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
              <td data-testid={`summary-status-${s}-count`}>
                {countWithStatus(claims.data, s)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 40, Component: ({ sheetId }) => <Summary sheetId={sheetId} /> });

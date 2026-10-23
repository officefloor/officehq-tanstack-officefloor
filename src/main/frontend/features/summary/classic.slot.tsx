import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { classicPeriodDueTotal, usePeriods } from '../periods/periods';
import { personDueTotal, sumDue, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';

/** The classic summary, shown beside the summary: per period its classic due-total (each claim counted by its status
 *  now; see classicPeriodDueTotal), and the sheet's due-total untrimmed: every person's due-total (as shown on their
 *  row) added up, skipping n/a. */
function ClassicSummary({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);

  return (
    <section data-testid="classic">
      <h2>Classic summary</h2>
      <table>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`classic-period-${p.name}`}>
              <th>Period {p.name}</th>
              <td>
                Due total{' '}
                <span data-testid={`classic-period-${p.name}-due-total`}>
                  {classicPeriodDueTotal(p, claims, people, rates, floor, late)}
                </span>
              </td>
            </tr>
          ))}
          <tr>
            <th>Sheet</th>
            <td>
              Due total{' '}
              <span data-testid="classic-due-total">
                {sumDue(people.map((p) => personDueTotal(p, claims, rates, today, floor, late, cap, people)))}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 24, Component: ClassicSummary });

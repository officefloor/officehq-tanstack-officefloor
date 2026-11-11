import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { heldParts, personDueTotal, sumDue, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { classicPeriodDueTotal, usePeriods } from '../periods/periods';
import { useCutTotals } from '../pools/pools';

// The classic summary, shown beside the summary: per period, its due-total counting each claim by
// its status now (a closed period shows the figure fixed when it closed), and the sheet's due-total
// with no trimming: everyone's due-total added up.
function ClassicSummary({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);

  const totals = people.map((p) =>
    personDueTotal(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutOf(p.id)),
  );

  return (
    <section data-testid="classic-summary">
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
              <td data-testid={`classic-period-${p.name}-due-total`}>{classicPeriodDueTotal(p, people, claims, rates, floor, late)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Due in all: <strong data-testid="classic-due-total">{sumDue(totals)}</strong>
      </p>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 30.5, Component: ClassicSummary });

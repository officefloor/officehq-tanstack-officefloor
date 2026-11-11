import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { sumDue, useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { useAsOf } from '../sheet/asof.slot';
import { STATUSES, sheetFigures, treeTotals, useDescendantFigures } from './consolidated';
import { usePools } from '../pools/pools';
import { useGradeCodes } from '../grades/grades';

// The consolidated figures, over the sheet and all its descendants: everyone on them, counted once
// each (people whose names or aliases match are one), and the due-total, overdue-total, settled-total,
// open-total and cut total of every
// person on them, with the single highest and lowest over the whole tree left out (three or more).
// With no children these are the header's. Per grade, the due-totals of the people
// on it (retired people left out), and per status the count of claims in it, added up over the tree. While the screen shows an as-of date,
// every sheet of the tree is shown as it stood at the end of that date, with that date as its today.
function Consolidated({ sheetId }: { sheetId: string }) {
  const GRADES = useGradeCodes(sheetId);
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const pools = usePools(sheetId).data ?? [];
  const [asOf] = useAsOf();
  const descendants = useDescendantFigures(sheetId, asOf).data ?? [];

  const own = sheetFigures(people, claims, rates, today, floor, late, cap, pools);
  const all = [own, ...descendants];
  const { people: count, dueTotal, overdueTotal, settledTotal, openTotal, cutTotal } = treeTotals(all);

  return (
    <section data-testid="consolidated">
      Consolidated — People: <strong data-testid="consolidated-people-count">{count}</strong>{' '}
      Due: <strong data-testid="consolidated-due-total">{dueTotal}</strong>{' '}
      Overdue: <strong data-testid="consolidated-overdue-total">{overdueTotal}</strong>{' '}
      Settled: <strong data-testid="consolidated-settled-total">{settledTotal}</strong>{' '}
      Open: <strong data-testid="consolidated-open-total">{openTotal}</strong>{' '}
      Cut: <strong data-testid="consolidated-cut-total">{cutTotal}</strong>
      <table>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>Grade {g}</td>
              <td data-testid={`consolidated-grade-${g}-due-total`}>{sumDue(all.map((f) => f.gradeDueTotals[g] ?? '0.00'))}</td>
            </tr>
          ))}
          {STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`consolidated-status-${s}-count`}>{all.reduce((n, f) => n + f.statusCounts[s], 0)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -99.5, Component: Consolidated });

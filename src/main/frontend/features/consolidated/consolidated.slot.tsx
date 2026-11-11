import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { sumDue, useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { useAsOf } from '../sheet/asof.slot';
import { sheetFigures, useDescendantFigures } from './consolidated';

// The consolidated figures, over the sheet and all its descendants: everyone on them, and each
// sheet's due-total and overdue-total as that sheet shows them, added up. The sheet's own figures
// are the header's; with no children these are just those. While the screen shows an as-of date,
// every sheet of the tree is shown as it stood at the end of that date, with that date as its today.
function Consolidated({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const [asOf] = useAsOf();
  const descendants = useDescendantFigures(sheetId, asOf).data ?? [];

  const own = sheetFigures(people, claims, rates, today, floor, late, cap);
  const all = [own, ...descendants];
  const count = all.reduce((n, f) => n + f.people, 0);

  return (
    <section data-testid="consolidated">
      Consolidated — People: <strong data-testid="consolidated-people-count">{count}</strong>{' '}
      Due: <strong data-testid="consolidated-due-total">{sumDue(all.map((f) => f.dueTotal))}</strong>{' '}
      Overdue: <strong data-testid="consolidated-overdue-total">{sumDue(all.map((f) => f.overdueTotal))}</strong>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -99.5, Component: Consolidated });

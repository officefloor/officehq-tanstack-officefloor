import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { sumDue, useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { useAsOf } from '../sheet/asof.slot';
import { sheetFigures, useDescendantFigures, useFiguresNow } from './consolidated';

// The consolidated figures, over the sheet and all its descendants: everyone on them, and each
// sheet's due-total and overdue-total as that sheet shows them, added up. The sheet's own figures
// are the header's; with no children these are just those. They are always the current figures: while
// the screen shows the sheet as of a date, the sheet's own are read again as it is now.
function Consolidated({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const descendants = useDescendantFigures(sheetId).data ?? [];
  const [asOf] = useAsOf();
  const now = useFiguresNow(sheetId, asOf !== '').data;

  const own = asOf !== '' ? now ?? { people: 0, dueTotal: '0.00', overdueTotal: '0.00' }
    : sheetFigures(people, claims, rates, today, floor, late, cap);
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

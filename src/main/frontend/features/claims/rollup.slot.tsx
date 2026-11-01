import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { twoDecimals, useRates } from '../rates/rates';
import { useFloorCents } from '../settings/settings';
import { claimed, counted, overdueCents, personDueCents, useClaims, useLate } from './claims';

// The sheet's claimed-total: everyone's counted (OPEN and SETTLED, but not an unbound OPEN one within its person's hold) claims added up; and its due-total: everyone's due-total
// added up, skipping the people whose due-total is n/a; and how many of the counted claims are overdue, with their
// dues (late charges included) added up.
function ClaimedTotal({ sheet }: { sheet: string; user: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const claims = counted(all, people);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const due = people.reduce((sum, p) => {
    const cents = personDueCents(all, rates, p, today, floor, late);
    return cents === undefined ? sum : sum + cents;
  }, 0n);
  const overdue = overdueCents(all, people, rates, late, floor);
  return (
    <>
      <p>
        Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims)}</strong>
      </p>
      <p>
        Due total: <strong data-testid="rollup-due-total">{twoDecimals(due)}</strong>
      </p>
      <p>
        Overdue: <strong data-testid="rollup-overdue-count">{overdue.count}</strong>, total{' '}
        <strong data-testid="rollup-overdue-total">{twoDecimals(overdue.cents)}</strong>
      </p>
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });

import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { twoDecimals, useRates } from '../rates/rates';
import { claimed, counted, personDueCents, useClaims } from './claims';

// The sheet's claimed-total: everyone's counted (OPEN and SETTLED, but not a held person's OPEN) claims added up; and its due-total: everyone's due-total
// added up, skipping the people whose due-total is n/a.
function ClaimedTotal({ sheet }: { sheet: string; user: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const claims = counted(all, people);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const due = people.reduce((sum, p) => {
    const cents = personDueCents(all, rates, p, today);
    return cents === undefined ? sum : sum + cents;
  }, 0n);
  return (
    <>
      <p>
        Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims)}</strong>
      </p>
      <p>
        Due total: <strong data-testid="rollup-due-total">{twoDecimals(due)}</strong>
      </p>
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });

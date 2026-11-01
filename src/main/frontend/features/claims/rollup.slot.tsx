import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { dueCents, rateOf, twoDecimals, useRates } from '../rates/rates';
import { claimed, useClaims } from './claims';

// The sheet's claimed-total: everyone's claims added up; and its due-total: everyone's due-total
// added up, skipping the people whose grade has no rate.
function ClaimedTotal({ sheet }: { sheet: string; user: string }) {
  const { data: claims = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const due = people.reduce((sum, p) => {
    const cents = dueCents(claimed(claims.filter((c) => c.person === p.id)), rateOf(rates, p.grade));
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

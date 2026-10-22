import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import { live, sumAmounts, useClaims } from './claims';

/** The sheet's claimed total: every person's claimed added up. */
function ClaimedTotal({ sheetId }: { sheetId: string; user: string }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  return (
    <p>
      Claimed total <strong data-testid="rollup-claimed-total">{sumAmounts(live(claims.data ?? [], people.data ?? []))}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 20, Component: ClaimedTotal });

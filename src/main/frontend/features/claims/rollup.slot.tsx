import { SheetBody } from '../../slots/defs/sheet';
import { sumAmounts, useClaims } from './claims';

/** The sheet's claimed total: every person's claimed added up. */
function ClaimedTotal({ sheetId }: { sheetId: string; user: string }) {
  const claims = useClaims(sheetId);
  return (
    <p>
      Claimed total <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [])}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 20, Component: ClaimedTotal });

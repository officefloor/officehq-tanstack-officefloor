import { SheetBody } from '../../slots/defs/sheetBody';
import { sumAmounts, useClaims } from './claims';

// The sheet's claimed-total: everyone's claims added up.
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  return (
    <p>
      Claimed in all: <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [])}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: ClaimedTotal });

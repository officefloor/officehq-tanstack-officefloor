import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople } from '../people/people';
import { sumAmounts, useClaims } from './claims';

// The sheet's claimed-total: everyone's claims added up.
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  const holds = new Map((usePeople(sheetId).data ?? []).map((p) => [p.id, p.holds]));
  return (
    <p>
      Claimed in all: <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [], (id) => holds.get(id) ?? [])}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: ClaimedTotal });

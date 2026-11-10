import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople } from '../people/people';
import { sumAmounts, useClaims } from './claims';

// The sheet's claimed-total: everyone's claims added up.
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  const held = new Set((usePeople(sheetId).data ?? []).filter((p) => p.held).map((p) => p.id));
  return (
    <p>
      Claimed in all: <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [], (id) => held.has(id))}</strong>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: ClaimedTotal });

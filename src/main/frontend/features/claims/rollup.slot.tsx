import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimed, useClaims } from './claims';

// The sheet's claimed-total: everyone's claims added up.
function ClaimedTotal({ sheet }: { sheet: string; user: string }) {
  const { data: claims = [] } = useClaims(sheet);
  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims)}</strong>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });

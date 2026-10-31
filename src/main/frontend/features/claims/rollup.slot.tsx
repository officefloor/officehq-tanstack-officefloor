import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimed, useClaims } from './useClaims';

/** The sheet's claimed-total: everyone's claims added up. */
function ClaimedTotal({ sheet }: { user: string; sheet: string }) {
  const { data: claims = [] } = useClaims(sheet);
  return (
    <p>
      Claimed in total: <strong data-testid="rollup-claimed-total">{claimed(claims)}</strong>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });

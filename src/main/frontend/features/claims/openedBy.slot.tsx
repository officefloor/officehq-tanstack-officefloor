import { ClaimCells } from '../../slots/defs/claimColumns';
import type { Claim } from './claimsQuery';

/** Who opened the claim (blank until it is opened). */
function ClaimOpenedBy({ claim }: { sheetId: string; claim: Claim }) {
  return <td data-testid="claim-opened-by">{claim.openedBy ?? ''}</td>;
}

export const contribution = ClaimCells.fill({ order: 25, Component: ClaimOpenedBy });

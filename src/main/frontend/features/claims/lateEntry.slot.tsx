import { ClaimCells } from '../../slots/defs/claimColumns';
import type { Claim } from './claimsQuery';

/** Whether the claim was added while its period was soft-closed. */
function ClaimLateEntry({ claim }: { sheetId: string; claim: Claim }) {
  return <td data-testid="claim-late-entry">{claim.lateEntry ? 'yes' : 'no'}</td>;
}

export const contribution = ClaimCells.fill({ order: 16, Component: ClaimLateEntry });

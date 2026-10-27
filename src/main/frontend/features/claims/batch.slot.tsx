import { ClaimCells } from '../../slots/defs/claimColumns';
import type { Claim } from './claimsQuery';

/** The number of the settle batch that settled the claim (blank when none, or settled one at a time). */
function ClaimBatch({ claim }: { sheetId: string; claim: Claim }) {
  return <td data-testid="claim-batch">{claim.batch ?? ''}</td>;
}

export const contribution = ClaimCells.fill({ order: 28, Component: ClaimBatch });

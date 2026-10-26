import { ClaimCells } from '../../slots/defs/claimColumns';
import type { Claim } from './claimsQuery';

/** The claim's status log, oldest first: each status it has had, by whom, on the sheet's today then, at its due then. */
function ClaimLog({ claim }: { sheetId: string; claim: Claim }) {
  return (
    <td data-testid="claim-log">
      {(claim.log ?? []).map((line, i) => (
        <div key={i} data-testid="claim-log-line">
          {line}
        </div>
      ))}
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 90, Component: ClaimLog });

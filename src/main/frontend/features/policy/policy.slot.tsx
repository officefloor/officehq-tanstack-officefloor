import { ClaimCells } from '../../slots/defs/claimColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import type { Claim } from '../claims/claimsQuery';
import { policyOn, usePolicyVersions } from '../settings/settingsQuery';

/** The sheet's current policy version: how many versions of its policy settings have been made. */
function PolicyVersion({ sheetId }: { sheetId: string }) {
  return (
    <p data-testid="policy-version-panel">
      Policy version <strong data-testid="policy-version">{usePolicyVersions(sheetId).length}</strong>
    </p>
  );
}

/** The policy version in force on the claim's date. */
function ClaimPolicy({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  return <td data-testid="claim-policy">{policyOn(usePolicyVersions(sheetId), claim.date)}</td>;
}

export const contributions = [
  SheetPanel.fill({ order: 25.6, Component: PolicyVersion }),
  ClaimCells.fill({ order: 10.5, Component: ClaimPolicy }),
];

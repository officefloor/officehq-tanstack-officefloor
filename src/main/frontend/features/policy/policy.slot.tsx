import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, type Claim } from '../claims/claimsQuery';
import { policyOn, usePolicyVersionLines, usePolicyVersions } from '../settings/settingsQuery';

/** The sheet's current policy version: how many versions of its policy settings have been made. */
function PolicyVersion({ sheetId }: { sheetId: string }) {
  return (
    <p data-testid="policy-version-panel">
      Policy version <strong data-testid="policy-version">{usePolicyVersions(sheetId).length}</strong>
    </p>
  );
}

/** The policy versions, oldest first, once shown: "<version> <from or start> <key>=<value> <claims under it>". */
function PolicyViewer({ sheetId }: { sheetId: string }) {
  const [shown, setShown] = useState(false);
  const versions = usePolicyVersions(sheetId);
  const lines = usePolicyVersionLines(sheetId);
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const under = new Map<number, number>();
  for (const claim of claims) {
    const no = policyOn(versions, claim.date);
    under.set(no, (under.get(no) ?? 0) + 1);
  }
  return (
    <section data-testid="policy-viewer">
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show policy versions
      </button>
      {shown && (
        <ol data-testid="policy-lines">
          {lines.map((v, i) => (
            <li key={i} data-testid="policy-line">
              {`${i + 1} ${v.from ?? 'start'} ${v.key}=${v.value} ${under.get(i + 1) ?? 0}`}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

/** The policy version in force on the claim's date. */
function ClaimPolicy({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  return <td data-testid="claim-policy">{policyOn(usePolicyVersions(sheetId), claim.date)}</td>;
}

export const contributions = [
  SheetPanel.fill({ order: 25.6, Component: PolicyVersion }),
  SheetPanel.fill({ order: 25.7, Component: PolicyViewer }),
  ClaimCells.fill({ order: 10.5, Component: ClaimPolicy }),
];

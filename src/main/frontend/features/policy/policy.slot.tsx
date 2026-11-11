import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSettings } from '../access/access';
import { claimsQuery } from '../claims/claims';
import { policyLine, usePolicyVersions } from './policy';

// The policy viewer: a show button, and once pressed one line per policy version, oldest first, each its
// number, its from-date (start for none), the setting it changed with its value, and the count of claims
// whose policy version it is. The lines are read again whenever the claims or the settings are.
function PolicyViewer({ sheetId }: { sheetId: string }) {
  const [shown, setShown] = useState(false);
  const settings = useSettings(sheetId);
  const claims = useQuery(claimsQuery(sheetId));
  const versions = usePolicyVersions(sheetId, Math.max(settings.dataUpdatedAt, claims.dataUpdatedAt));

  return (
    <section data-testid="policy">
      <h2>Policy versions</h2>
      <button
        type="button"
        data-testid="policy-show"
        onClick={() => {
          setShown(true);
          void versions.refetch();
        }}
      >
        Show policy versions
      </button>
      {shown && (
        <ul data-testid="policy-list">
          {(versions.data ?? []).map((v) => (
            <li key={v.version} data-testid="policy-line">
              {policyLine(v)}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -8.5, Component: PolicyViewer });

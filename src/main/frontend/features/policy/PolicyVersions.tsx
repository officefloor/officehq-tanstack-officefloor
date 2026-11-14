import { useState } from 'react';
import { useClaims } from '../claims/claims';
import { useSettings } from '../settings/settings';
import { policyVersionOn } from './policy';

// The sheet's policy versions, oldest first, once shown: each its number, its from-date ("start" when it has none), the
// setting and value it set, and how many claims it is the policy version of (the one in force on the claim's date).
export function PolicyVersions() {
  const [shown, setShown] = useState(false);
  const versions = useSettings().data?.policyVersions ?? [];
  const claims = (useClaims().data ?? []).filter((c) => c.status !== 'DELETED');

  return (
    <div>
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show policy versions
      </button>
      {shown && (
        <ul>
          {versions.map((v) => (
            <li key={v.number} data-testid="policy-line">
              {v.number} {v.from || 'start'} {v.key}={v.value}{' '}
              {claims.filter((c) => policyVersionOn(versions, c.date) === v.number).length}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

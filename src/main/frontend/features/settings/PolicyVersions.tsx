import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { claimsKey, listClaims } from '../claims/api';
import { listPolicyVersions, policyVersionsKey, usePolicyVersions } from './api';

/** The sheet's policy versions, oldest first, once shown: each one's number, its from-date (start:
 *  always), the setting and value its change set, and how many claims go by it (the version in force on
 *  their date). */
export function PolicyVersions({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [shown, setShown] = useState(false);
  const versions = useQuery({ queryKey: policyVersionsKey(sheetId), queryFn: () => listPolicyVersions(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const policy = usePolicyVersions(sheetId);
  const count = (version: number) => (claims.data ?? []).filter((c) => policy.on(c.date) === version).length;

  return (
    <section>
      <h3>Policy versions</h3>
      <button type="button" data-testid="policy-show" onClick={() => {
        setShown(true);
        void queryClient.invalidateQueries({ queryKey: policyVersionsKey(sheetId) });
        void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId), exact: true });
      }}>
        Show policy versions
      </button>
      {shown && (
        <ul>
          {(versions.data ?? []).map((v) => (
            <li key={v.version} data-testid="policy-line">
              {v.version} {v.from ?? 'start'} {v.key}={v.value} {count(v.version)}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

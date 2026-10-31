import { useState } from 'react';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useClaims } from '../claims/useClaims';
import { usePolicyVersions, useSettings } from './useSettings';

// The policy versions, shown on demand: one line per version, oldest first, as
// "<version> <from> <key>=<value> <count of claims whose policy version it is>".
export function PolicyViewer() {
  const [sheet] = useSearchParam('sheet', asString);
  const settings = useSettings(sheet);
  const claims = useClaims(sheet);
  const versions = usePolicyVersions(sheet);
  const [shown, setShown] = useState(false);
  const counts = new Map<number, number>();
  for (const c of claims) {
    if (c.status === 'DELETED') continue;
    const v = versions.on(c.date);
    counts.set(v, (counts.get(v) ?? 0) + 1);
  }
  return (
    <>
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>Show policy versions</button>
      {shown && (
        <ul data-testid="policy-lines">
          {(settings?.policyLines ?? []).map((l, i) => (
            <li key={i} data-testid="policy-line">{`${i + 1} ${l.from || 'start'} ${l.key}=${l.value} ${counts.get(i + 1) ?? 0}`}</li>
          ))}
        </ul>
      )}
    </>
  );
}

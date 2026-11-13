import { useState } from 'react';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';
import { useClaims } from '../claims/claims';
import { policyOf, useSettings } from './settings';

// The sheet's policy versions, one line per version, oldest first: the version, its from-date (start for one with
// none), the key=value it changed, and how many of the sheet's claims have it as their policy version (the version
// in force on the claim's date, as each claim row shows it).
function PolicyViewer() {
  const [sheet] = useCurrentSheet();
  const settings = useSettings(sheet);
  const claims = useClaims(sheet);
  const [shown, setShown] = useState(false);
  const policy = policyOf(settings.data);
  const counts = new Map<number, number>();
  for (const c of claims.data ?? []) {
    const version = policy?.(c.date) ?? 0;
    counts.set(version, (counts.get(version) ?? 0) + 1);
  }
  const froms = settings.data?.policyFrom ?? [];
  const changes = settings.data?.policyChange ?? [];
  return (
    <section data-testid="policy">
      <h2>Policy versions</h2>
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show policy versions
      </button>
      {shown && (
        <ul>
          {changes.map((change, i) => (
            <li key={i + 1} data-testid="policy-line">
              {`${i + 1} ${froms[i] ?? 'start'} ${change} ${counts.get(i + 1) ?? 0}`}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 49, Component: PolicyViewer });

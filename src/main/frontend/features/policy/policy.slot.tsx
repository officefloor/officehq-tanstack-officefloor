import { useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useClaims } from '../claims/claims';
import { usePolicyVersionOn, useSettings } from '../settings/settings';

// The sheet's policy versions, oldest first, one line each: its number, its from-date (or "start" when it has none),
// the key and value its change set, and how many claims have it as their policy version (the version in force on the
// claim's date, as each claim row shows it).
function Policy({ sheet }: { sheet: string; user: string }) {
  const [shown, setShown] = useState(false);
  const { data: settings } = useSettings(sheet);
  const { data: claims } = useClaims(sheet);
  const policyOn = usePolicyVersionOn(sheet);

  const counts = new Map<number, number>();
  for (const claim of claims ?? []) {
    const version = policyOn(claim.date);
    counts.set(version, (counts.get(version) ?? 0) + 1);
  }

  return (
    <section>
      <h2>Policy versions</h2>
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show
      </button>
      {shown && (
        <ul>
          {(settings?.policyChanges ?? []).map((v, i) => (
            <li key={i} data-testid="policy-line">
              <code>{`${i + 1} ${v.from === '' ? 'start' : v.from} ${v.key}=${v.value} ${counts.get(i + 1) ?? 0}`}</code>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 30, Component: Policy });

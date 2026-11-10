import { useState } from 'react';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { policyVersionOn, useSettings } from '../settings/useSettings';

/** The sheet's policy versions, shown on request: one line per version, oldest first, with its from-date (start when
 *  it has none), the setting it changed and its value, and how many claims have it as their policy version. */
function PolicyViewer({ sheetId }: { sheetId: string }) {
  const [shown, setShown] = useState(false);
  const settings = useSettings(sheetId);
  const claims = useClaims(sheetId);
  const counts = new Map<number, number>();
  if (settings.data !== undefined) {
    for (const c of claims.data ?? []) {
      const v = policyVersionOn(settings.data, c.date);
      counts.set(v, (counts.get(v) ?? 0) + 1);
    }
  }
  return (
    <section>
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show policy versions
      </button>
      {shown && (
        <ul>
          {(settings.data?.policyVersionDetails ?? []).map((p) => (
            <li key={p.version} data-testid="policy-line">
              {p.version} {p.from ?? 'start'} {p.key}={p.value ?? ''} {counts.get(p.version) ?? 0}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 33, Component: ({ sheetId }) => <PolicyViewer sheetId={sheetId} /> });

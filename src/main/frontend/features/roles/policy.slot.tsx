import { useState } from 'react';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useClaims } from '../claims/claims';
import { policyVersionOn, useSettings } from './roles';

// The policy versions, shown once the button is pressed: one line per version, oldest first,
// <version> <from-date or start> <key>=<value> <count of claims (DELETED ones left out) whose policy version it is>.
function PolicyVersions() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings();
  const claims = useClaims();
  const [shown, setShown] = useState(false);
  if (!user || !sheet) return null;
  const counted = (claims.data ?? []).filter((c) => c.status !== 'DELETED');
  const lines = (settings.data?.policyLog ?? []).map((v, i) => {
    const count = counted.filter((c) => policyVersionOn(settings.data, c.date) === i + 1).length;
    return `${i + 1} ${v.from ?? 'start'} ${v.key}=${v.value ?? ''} ${count}`;
  });
  return (
    <section data-testid="policy">
      <button type="button" data-testid="policy-show" onClick={() => setShown(true)}>
        Show policy versions
      </button>
      {shown && (
        <ul>
          {lines.map((l) => <li key={l} data-testid="policy-line">{l}</li>)}
        </ul>
      )}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 34.7, Component: PolicyVersions });

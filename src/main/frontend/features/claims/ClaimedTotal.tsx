import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimed, useClaims } from './claims';

// The sheet's claimed-total: everyone's claims added up.
export function ClaimedTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = useClaims();

  if (!user || !sheet) return null;

  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims.data ?? [])}</strong>
    </p>
  );
}

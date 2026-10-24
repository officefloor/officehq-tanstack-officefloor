import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import { holdsOf, sumOf, useClaims } from './claims';

// The sheet's claimed-total: everyone's claimed added up.
function ClaimedTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  if (!user || !sheet) return null;
  return (
    <p>
      Claimed in total <strong data-testid="rollup-claimed-total">{sumOf(claims.data ?? [], holdsOf(people.data))}</strong>
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 30, Component: ClaimedTotal });

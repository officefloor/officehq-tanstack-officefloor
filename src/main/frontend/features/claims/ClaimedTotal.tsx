import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import { claimed, holdsById, useClaims } from './claims';

// The sheet's claimed-total: everyone's counting claims added up (open ones a hold covers left out).
export function ClaimedTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = useClaims();
  const people = usePeople().data ?? [];

  if (!user || !sheet) return null;

  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims.data ?? [], holdsById(people))}</strong>
    </p>
  );
}

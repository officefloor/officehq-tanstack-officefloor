import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useSheetToday } from '../session/sheetToday';
import { claimed, useClaims } from './claims';
import { usePeople } from '../people/people';

// The sheet's claimed-total: everyone's counting claims added up (corrections left out; held ones count; not
// a retired person's once their retirement date is reached).
export function ClaimedTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = useClaims();
  const today = useSheetToday();
  const people = usePeople().data ?? [];

  if (!user || !sheet) return null;

  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims.data ?? [], today, people)}</strong>
    </p>
  );
}

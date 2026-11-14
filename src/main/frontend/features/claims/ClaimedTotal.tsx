import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useSheetToday } from '../session/sheetToday';
import { claimed, useClaims } from './claims';

// The sheet's claimed-total: everyone's counting claims added up (corrections left out; held ones count).
export function ClaimedTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = useClaims();
  const today = useSheetToday();

  if (!user || !sheet) return null;

  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims.data ?? [], today)}</strong>
    </p>
  );
}

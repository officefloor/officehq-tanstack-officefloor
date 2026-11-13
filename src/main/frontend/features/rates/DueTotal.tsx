import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useSheetToday } from '../session/sheetToday';
import { useFloor } from '../settings/settings';
import { personDue, total, useRates } from './rates';

// The sheet's due-total: everyone's due-total added up, skipping those that are n/a.
export function DueTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const today = useSheetToday();
  const floor = useFloor();

  if (!user || !sheet) return null;

  const dues = people.map((p) => personDue(claims.filter((c) => c.personId === p.id), rates, p.grade, today, p.held, floor));
  return (
    <p>
      Due total: <strong data-testid="rollup-due-total">{total(dues)}</strong>
    </p>
  );
}

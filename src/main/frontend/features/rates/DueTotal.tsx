import { claimed, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { due, rateFor, total, useRates } from './rates';

// The sheet's due-total: everyone's due-total added up, skipping those with no rate.
export function DueTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];

  if (!user || !sheet) return null;

  const dues = people.map((p) =>
    due(claimed(claims.filter((c) => c.personId === p.id)), rateFor(rates, p.grade)),
  );
  return (
    <p>
      Due total: <strong data-testid="rollup-due-total">{total(dues)}</strong>
    </p>
  );
}

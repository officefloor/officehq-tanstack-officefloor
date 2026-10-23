import { PersonRow } from '../../slots/defs/personRow';
import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { personScore, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';

/** A person's score: their settled figures divided by 100, minus 5 per overdue claim that counts, rounded half to
 *  even (see personScore). */
function PersonScore({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return (
    <td>
      Score <span data-testid="person-score">{personScore(person, claims, rates, today, floor, late)}</span>
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 24, Component: PersonScore });

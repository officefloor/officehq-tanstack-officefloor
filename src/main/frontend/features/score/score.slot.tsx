import { PersonRow } from '../../slots/defs/personRow';
import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { personScore, useRates } from '../rates/rates';
import { useFloor, useLate, useScoreThresholds } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';

/** A person's band by their score: HIGH at or above the high threshold, MID at or above the mid one, else LOW; ''
 *  while either threshold is blank. */
export function scoreBand(score: string, mid: string, high: string): string {
  if (mid === '' || high === '') return '';
  const n = BigInt(score);
  if (n >= BigInt(high)) return 'HIGH';
  if (n >= BigInt(mid)) return 'MID';
  return 'LOW';
}

/** A person's score: their settled figures divided by 100, minus 5 per overdue claim that counts, rounded half to
 *  even (see personScore), and their band by the sheet's score thresholds (see scoreBand). */
function PersonScore({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const { mid, high } = useScoreThresholds(sheetId);
  const score = personScore(person, claims, rates, today, floor, late);
  return (
    <td>
      Score <span data-testid="person-score">{score}</span>{' '}
      Band <span data-testid="person-band">{scoreBand(score, mid, high)}</span>
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 24, Component: PersonScore });

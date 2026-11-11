import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate, useScoreThresholds } from '../access/access';
import { personScore, scoreBand } from './score';

function ScoreHead() {
  return (
    <>
      <th>Score</th>
      <th>Band</th>
    </>
  );
}

// A person's score: their settled due / 100, minus 5 per overdue claim that counts, rounded half to even, and its band
// by the sheet's score thresholds (blank while either is).
function PersonScore({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const { mid, high } = useScoreThresholds(sheetId);
  const score = personScore(rates, person.grades, today, claims, liveHolds(person, today), floor, late);
  return (
    <>
      <td data-testid="person-score">{score}</td>
      <td data-testid="person-band">{scoreBand(score, mid, high)}</td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 25.5, Component: ScoreHead }),
  PersonRow.fill({ order: 25.5, Component: PersonScore }),
];

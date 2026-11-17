import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { live, useClaims } from '../claims/claims';
import { overdue } from '../overdue/overdue';
import { type Person } from '../people/people';
import { personSplit, useRates } from '../rates/rates';
import { useFloor, useLate, useScoreThresholds } from '../settings/settings';
import { band, score } from './score';

function ScoreHead() {
  return (
    <>
      <th>Score</th>
      <th>Band</th>
    </>
  );
}

/** A person's score (see score), and their band by it (see band), over their claims that count in the live totals. */
function PersonScore({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const mine = live(claims.data?.filter((c) => c.personId === person.id) ?? [], [person], late.today);
  const settled = personSplit(mine, rates.data, person, late, floor).settled;
  const count = overdue(mine, rates.data, person, late, floor).count;
  const thresholds = useScoreThresholds(sheetId);
  const shown = score(settled, count);
  return (
    <>
      <td data-testid="person-score">{shown}</td>
      <td data-testid="person-band">{band(shown, thresholds.mid, thresholds.high)}</td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 26, Component: ScoreHead }),
  PersonRow.fill({ order: 26, Component: PersonScore }),
];

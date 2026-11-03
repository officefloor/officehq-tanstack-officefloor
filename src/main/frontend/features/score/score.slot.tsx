import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { useSettings } from '../settings/useSettings';
import { useScores } from './useScore';

/** A person's score: their settled figures divided by 100, minus 5 per overdue claim that counts, rounded
 *  half to even to a whole number. */
function PersonScore({ sheetId, personId }: { sheetId: string; personId: number }) {
  const score = useScores(sheetId).get(personId);
  return <td data-testid="person-score">{score === undefined ? '' : score.toString()}</td>;
}

/** A person's band, while both score thresholds are set: HIGH at or above the high one, MID at or above the mid
 *  one, else LOW. Blank while either threshold is blank or the score is not yet known. */
function PersonBand({ sheetId, personId }: { sheetId: string; personId: number }) {
  const score = useScores(sheetId).get(personId);
  const settings = useSettings(sheetId).data;
  const mid = settings?.scoreMid;
  const high = settings?.scoreHigh;
  let band = '';
  if (score !== undefined && mid != null && high != null) {
    band = score >= BigInt(high) ? 'HIGH' : score >= BigInt(mid) ? 'MID' : 'LOW';
  }
  return <td data-testid="person-band">{band}</td>;
}

export const contributions = [
  PersonColumnHead.fill({ order: 26, Component: () => <th>Score</th> }),
  PersonCells.fill({ order: 26, Component: PersonScore }),
  PersonColumnHead.fill({ order: 26.5, Component: () => <th>Band</th> }),
  PersonCells.fill({ order: 26.5, Component: PersonBand }),
];

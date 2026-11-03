import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { useScores } from './useScore';

/** A person's score: their settled figures divided by 100, minus 5 per overdue claim that counts, rounded
 *  half to even to a whole number. */
function PersonScore({ sheetId, personId }: { sheetId: string; personId: number }) {
  const score = useScores(sheetId).get(personId);
  return <td data-testid="person-score">{score === undefined ? '' : score.toString()}</td>;
}

export const contributions = [
  PersonColumnHead.fill({ order: 26, Component: () => <th>Score</th> }),
  PersonCells.fill({ order: 26, Component: PersonScore }),
];

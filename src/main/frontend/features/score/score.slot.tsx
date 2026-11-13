import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person } from '../people/people';
import { useSplit } from '../rates/split.slot';
import { useOverdue } from '../overdue/overdue.slot';
import { useSettings } from '../settings/settings';
import { useCurrentSheet } from '../../session/session';

// A person's score: their settled-total divided by 100, minus 5 for every overdue claim of theirs that counts,
// rounded half to even to a whole number.
function ScoreHead() {
  return <th>Score</th>;
}

/** The score from a settled-total (a 2-place figure) and an overdue count. */
export function score(settled: string, overdue: number): string {
  const [whole, frac = ''] = settled.replace('-', '').split('.');
  const sign = settled.startsWith('-') ? -1n : 1n;
  // In ten-thousandths of a point: settled (in hundredths) less 5 points per overdue claim.
  const n = sign * BigInt(whole + frac.padEnd(2, '0').slice(0, 2)) - BigInt(overdue) * 50000n;
  const q = n / 10000n;
  const r = n % 10000n;
  const twice = 2n * (r < 0n ? -r : r);
  const step = n < 0n ? -1n : 1n;
  const rounded = twice > 10000n || (twice === 10000n && q % 2n !== 0n) ? q + step : q;
  return rounded.toString();
}

function PersonScore({ person }: { person: Person }) {
  const { settled } = useSplit()(person);
  const { count } = useOverdue()(person);
  return <td data-testid="person-score">{score(settled, count)}</td>;
}

function BandHead() {
  return <th>Band</th>;
}

/** A person's band from their score: HIGH at or above the high threshold, MID at or above the mid one, else LOW;
 *  blank while either threshold is. */
export function band(score: string, mid: string | null | undefined, high: string | null | undefined): string {
  if (mid == null || high == null) return '';
  const s = BigInt(score);
  return s >= BigInt(high) ? 'HIGH' : s >= BigInt(mid) ? 'MID' : 'LOW';
}

function PersonBand({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const settings = useSettings(sheet);
  const { settled } = useSplit()(person);
  const { count } = useOverdue()(person);
  return <td data-testid="person-band">{band(score(settled, count), settings.data?.scoreMid, settings.data?.scoreHigh)}</td>;
}

export const contributions = [
  PersonColumnHeads.fill({ order: 17.2, Component: ScoreHead }),
  PersonColumns.fill({ order: 17.2, Component: PersonScore }),
  PersonColumnHeads.fill({ order: 17.3, Component: BandHead }),
  PersonColumns.fill({ order: 17.3, Component: PersonBand }),
];

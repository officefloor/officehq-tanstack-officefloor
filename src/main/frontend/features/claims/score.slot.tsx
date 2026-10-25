import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useSheetToday } from '../session/session';
import { type Person } from '../people/people';
import { useSettings } from '../roles/roles';
import { useRates } from '../rates/rates';
import { splitOf } from '../rates/split.slot';
import { overdueOf } from './overdue.slot';
import { useClaims } from './claims';

// A person's score: their settled-total divided by 100, minus 5 for each counted overdue claim, rounded half to even
// to a whole number. Worked in hundredths of the score so the rounding is exact.
export function scoreOf(settledTotal: string, overdueCount: number): string {
  const [whole, frac = '00'] = settledTotal.replace('-', '').split('.');
  const cents = BigInt(whole) * 100n + BigInt(frac.padEnd(2, '0').slice(0, 2));
  // the score in ten-thousandths: cents / 10000 is the settled-total / 100
  const n = (settledTotal.startsWith('-') ? -cents : cents) - 50000n * BigInt(overdueCount);
  const negative = n < 0n;
  const m = negative ? -n : n;
  let q = m / 10000n;
  const r = m % 10000n;
  if (r > 5000n || (r === 5000n && q % 2n === 1n)) q += 1n;
  return `${negative && q !== 0n ? '-' : ''}${q}`;
}

function ScoreHeader() {
  return <th>Score</th>;
}

function PersonScore({ person }: { person: Person }) {
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  const all = claims.data ?? [];
  const { settled } = splitOf(person, all, rates.data, today, settings.data);
  const { count } = overdueOf(person, all, rates.data, today, settings.data);
  return <td data-testid="person-score">{scoreOf(settled, count)}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 16.5, Component: ScoreHeader }),
  PersonCells.fill({ order: 16.5, Component: PersonScore }),
];

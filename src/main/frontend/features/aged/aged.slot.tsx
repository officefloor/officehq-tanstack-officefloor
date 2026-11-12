import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { HomeMain } from '../../slots/defs/homeMain';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { type Claim, counts, isOpen, live, useClaims } from '../claims/claims';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { claimDue, dueTotal, useRates } from '../rates/rates';
import { useFigure } from '../locale/locale';

// A person's due of their OPEN (and PENDING) claims that count, split by the claim's age on the sheet's
// today (the days from its date): 0 to 30, 31 to 60, 61 or more. A claim dated after today is 0 to 30.
const DAY = 24 * 60 * 60 * 1000;

/** The claim's age in days on the sheet's today; 0 when it is dated after it. */
function age(claim: Claim, today: string): number {
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${claim.date}T00:00:00Z`)) / DAY;
  return Math.max(0, Math.round(days));
}

function AgedHead() {
  return (
    <>
      <th>Aged 0–30</th>
      <th>Aged 31–60</th>
      <th>Aged 61+</th>
    </>
  );
}

type Aged = { young: string; middle: string; old: string };

/** A person's aged due on the sheet (the current sheet's unless another is named). */
function useAged(of?: string) {
  const [current] = useCurrentSheet();
  const sheet = of ?? current;
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  return (person: Person): Aged => {
    const late = lateOf(settings, today, [person]);
    const mine = live((claims.data ?? []).filter((c) => c.personId === person.id), [person])
      .filter((c) => isOpen(c) && counts(c));
    const bucket = (from: number, to: number) =>
      dueTotal(
        today === undefined
          ? []
          : mine
              .filter((c) => age(c, today) >= from && age(c, today) <= to)
              .map((c) => claimDue(c, rates.data, person.grades, floor, late)),
      );
    return { young: bucket(0, 30), middle: bucket(31, 60), old: bucket(61, Infinity) };
  };
}

function PersonAged({ person }: { person: Person }) {
  const fig = useFigure();
  const { young, middle, old } = useAged()(person);
  return (
    <>
      <td data-testid="person-aged-0-30">{fig(young)}</td>
      <td data-testid="person-aged-31-60">{fig(middle)}</td>
      <td data-testid="person-aged-61">{fig(old)}</td>
    </>
  );
}

// The sheet's aged due: everyone's buckets added up, not trimmed.
function SheetAged() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const figures = (people.data ?? []).map(useAged(sheet));
  const sum = (key: keyof Aged) => dueTotal(figures.map((f) => f[key]));
  return (
    <p>
      Aged 0–30 <strong data-testid="rollup-aged-0-30">{fig(sum('young'))}</strong>{' '}
      Aged 31–60 <strong data-testid="rollup-aged-31-60">{fig(sum('middle'))}</strong>{' '}
      Aged 61+ <strong data-testid="rollup-aged-61">{fig(sum('old'))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 17.5, Component: AgedHead }),
  PersonColumns.fill({ order: 17.5, Component: PersonAged }),
  HomeMain.fill({ order: 36.4, Component: SheetAged }),
];

import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { live, useClaims } from '../claims/claims';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { dueTotal, personSplit, useRates } from './rates';
import { useFigure } from '../locale/locale';

// A person's settled-total and open-total (their due-total before the cap, split by status), and the
// sheet's: everyone's added up, not trimmed.
function SplitHead() {
  return (
    <>
      <th>Settled total</th>
      <th>Open total</th>
    </>
  );
}

export function useSplit() {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  return (person: Person) =>
    personSplit(
      live((claims.data ?? []).filter((c) => c.personId === person.id), [person]),
      rates.data,
      person.grades,
      today,
      floorOf(settings),
      lateOf(settings, today, [person]),
    );
}

function PersonSplit({ person }: { person: Person }) {
  const fig = useFigure();
  const { settled, open } = useSplit()(person);
  return (
    <>
      <td data-testid="person-settled-total">{fig(settled)}</td>
      <td data-testid="person-open-total">{fig(open)}</td>
    </>
  );
}

function SplitTotal() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const figures = (people.data ?? []).map(useSplit());
  return (
    <p>
      Settled total <strong data-testid="rollup-settled-total">{fig(dueTotal(figures.map((f) => f.settled)))}</strong>{' '}
      Open total <strong data-testid="rollup-open-total">{fig(dueTotal(figures.map((f) => f.open)))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 15.2, Component: SplitHead }),
  PersonColumns.fill({ order: 15.2, Component: PersonSplit }),
  HomeMain.fill({ order: 35.5, Component: SplitTotal }),
];

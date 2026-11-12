import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { counts, live, useClaims } from '../claims/claims';
import { useSettings } from '../settings/settings';
import { dueTotal, useRates } from '../rates/rates';
import { lateOf, overdueFigures } from './overdue';

// A person's overdue count and overdue-total (the due of their overdue claims that count), and the sheet's.
function OverdueHead() {
  return (
    <>
      <th>Overdue</th>
      <th>Overdue total</th>
    </>
  );
}

function useOverdue() {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  return (person: Person) =>
    overdueFigures(
      live((claims.data ?? []).filter((c) => c.personId === person.id), [person]).filter(counts),
      rates.data,
      person.grades,
      settings?.floor,
      today,
      lateOf(settings, today),
    );
}

function PersonOverdue({ person }: { person: Person }) {
  const { count, total } = useOverdue()(person);
  return (
    <>
      <td data-testid="person-overdue-count">{count}</td>
      <td data-testid="person-overdue-total">{total}</td>
    </>
  );
}

function OverdueTotal() {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const figures = (people.data ?? []).map(useOverdue());
  return (
    <p>
      Overdue <strong data-testid="rollup-overdue-count">{figures.reduce((n, f) => n + f.count, 0)}</strong>{' '}
      Overdue total <strong data-testid="rollup-overdue-total">{dueTotal(figures.map((f) => f.total))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 17, Component: OverdueHead }),
  PersonColumns.fill({ order: 17, Component: PersonOverdue }),
  HomeMain.fill({ order: 36, Component: OverdueTotal }),
];

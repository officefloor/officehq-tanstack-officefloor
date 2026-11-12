import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { counts, live, useClaims } from '../claims/claims';
import { floorOf, useSettings } from '../settings/settings';
import { trimmedTotal, useRates } from '../rates/rates';
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

function useOverdue(of?: string) {
  const [current] = useCurrentSheet();
  const sheet = of ?? current;
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  return (person: Person) =>
    overdueFigures(
      live((claims.data ?? []).filter((c) => c.personId === person.id), [person]).filter(counts),
      rates.data,
      person.grades,
      floorOf(settings),
      today,
      lateOf(settings, today, [person]),
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

/** The sheet's overdue figures (the current sheet's unless another is named): everyone's overdue counts
 *  added up, and their overdue-totals added up trimmed of the single highest and lowest when three or more
 *  people have one. */
export function useSheetOverdue(of?: string): { count: number; total: string } {
  const [current] = useCurrentSheet();
  const sheet = of ?? current;
  const people = usePeople(sheet);
  const figures = (people.data ?? []).map(useOverdue(sheet));
  return { count: figures.reduce((n, f) => n + f.count, 0), total: trimmedTotal(figures.map((f) => f.total)) };
}

function OverdueTotal() {
  const { count, total } = useSheetOverdue();
  return (
    <p>
      Overdue <strong data-testid="rollup-overdue-count">{count}</strong>{' '}
      Overdue total <strong data-testid="rollup-overdue-total">{total}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 17, Component: OverdueHead }),
  PersonColumns.fill({ order: 17, Component: PersonOverdue }),
  HomeMain.fill({ order: 36, Component: OverdueTotal }),
];

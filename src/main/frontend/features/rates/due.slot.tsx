import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { live, useClaims } from '../claims/claims';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { type Shared, capLess, personDue, personExcess, sharedParts, trimmedTotal, useRates } from './rates';
import { cutTotal, usePools } from '../pools/pools';
import { useFigure } from '../locale/locale';
import { useColumnShown } from '../columns/columns';

// A person's due-total (the sum of their counting claims' rounded figures, at most the sheet's cap less their cut
// total), their excess over that, and the sheet's due-total (everyone's, skipping n/a, trimmed).
function DueHead() {
  if (!useColumnShown()('due-total')) return null;
  return <th>Due total</th>;
}

function PersonDue({ person }: { person: Person }) {
  const shown = useColumnShown();
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  const shared = useShared()(person);
  const cap = useCapOf()(person);
  const mine = live((claims.data ?? []).filter((c) => c.personId === person.id), [person]);
  if (!shown('due-total')) return null;
  return <td data-testid="person-due-total">{fig(personDue(mine, rates.data, person.grades, today, floor, lateOf(settings, today, [person]), cap, shared))}</td>;
}

function ExcessHead() {
  if (!useColumnShown()('excess')) return null;
  return <th>Excess</th>;
}

function PersonExcess({ person }: { person: Person }) {
  const shown = useColumnShown();
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const shared = useShared()(person);
  const cap = useCapOf()(person);
  const mine = live((claims.data ?? []).filter((c) => c.personId === person.id), [person]);
  if (!shown('excess')) return null;
  return (
    <td data-testid="person-excess">
      {fig(personExcess(mine, rates.data, person.grades, today, floorOf(settings), lateOf(settings, today, [person]), cap, shared))}
    </td>
  );
}

/** The sheet's due-total (the current sheet's unless another is named): everyone's (retired people included)
 *  due-totals added up, skipping n/a, trimmed of the single highest and lowest when three or more people have one. */
export function useSheetDueTotal(of?: string): string {
  const [current] = useCurrentSheet();
  const sheet = of ?? current;
  const people = usePeople(sheet);
  return trimmedTotal((people.data ?? []).map(usePersonDue(sheet)));
}

/** A person's due-total on the sheet (the current sheet's unless another is named), as their row shows it. */
export function usePersonDue(of?: string): (person: Person) => string {
  const [current] = useCurrentSheet();
  const sheet = of ?? current;
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  const shared = useShared(sheet);
  const capOf = useCapOf(sheet);
  return (p: Person) =>
    personDue(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), rates.data, p.grades, today, floor, lateOf(settings, today, [p]), capOf(p), shared(p));
}

/** The cap each person's due-total on the sheet (the current sheet's unless another is named) is held to: the
 *  sheet's cap less their cut total (see `capLess`). */
export function useCapOf(of?: string): (person: Person) => string | null | undefined {
  const [current] = useCurrentSheet();
  const sheet = of ?? current;
  const cap = useSettings(sheet).data?.cap;
  const pools = usePools(sheet).data ?? [];
  return (p: Person) => capLess(cap, cutTotal(pools, p.id));
}

/** What claims shared with or by each person on the sheet (the current sheet's unless another is named) move in
 *  their figures (see `sharedParts`); undefined for a person no shared claim touches. */
export function useShared(of?: string): (person: Person) => Shared | undefined {
  const [current] = useCurrentSheet();
  const sheet = of ?? current;
  const people = usePeople(sheet).data ?? [];
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const parts = sharedParts(live(claims.data ?? [], people), people, rates.data, floorOf(settings), lateOf(settings, today, people), today);
  return (p: Person) => parts.get(p.id);
}

function DueTotal() {
  const fig = useFigure();
  return (
    <p>
      Due total <strong data-testid="rollup-due-total">{fig(useSheetDueTotal())}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 15, Component: DueHead }),
  PersonColumns.fill({ order: 15, Component: PersonDue }),
  PersonColumnHeads.fill({ order: 15.5, Component: ExcessHead }),
  PersonColumns.fill({ order: 15.5, Component: PersonExcess }),
  HomeMain.fill({ order: 35, Component: DueTotal }),
];

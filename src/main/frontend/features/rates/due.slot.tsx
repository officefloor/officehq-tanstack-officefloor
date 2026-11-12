import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { live, useClaims } from '../claims/claims';
import { useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { dueTotal, personDue, personExcess, useRates } from './rates';

// A person's due-total (the sum of their counting claims' rounded figures, at most the sheet's cap), their
// excess over the cap, and the sheet's due-total (everyone's, skipping n/a).
function DueHead() {
  return <th>Due total</th>;
}

function PersonDue({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = settings?.floor;
  const mine = live((claims.data ?? []).filter((c) => c.personId === person.id), [person]);
  return <td data-testid="person-due-total">{personDue(mine, rates.data, person.grades, today, floor, lateOf(settings, today), settings?.cap)}</td>;
}

function ExcessHead() {
  return <th>Excess</th>;
}

function PersonExcess({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const mine = live((claims.data ?? []).filter((c) => c.personId === person.id), [person]);
  return (
    <td data-testid="person-excess">
      {personExcess(mine, rates.data, person.grades, today, settings?.floor, lateOf(settings, today), settings?.cap)}
    </td>
  );
}

/** The sheet's due-total: everyone's (retired people included) due-totals added up, skipping n/a. */
export function useSheetDueTotal(): string {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = settings?.floor;
  const dues = (people.data ?? []).map((p) =>
    personDue(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), rates.data, p.grades, today, floor, lateOf(settings, today), settings?.cap),
  );
  return dueTotal(dues);
}

function DueTotal() {
  return (
    <p>
      Due total <strong data-testid="rollup-due-total">{useSheetDueTotal()}</strong>
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

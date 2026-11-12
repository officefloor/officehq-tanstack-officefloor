import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { live, useClaims } from '../claims/claims';
import { useSettings } from '../settings/settings';
import { dueTotal, personDue, useRates } from './rates';

// A person's due-total (the sum of their counting claims' rounded figures) and the sheet's (everyone's, skipping n/a).
function DueHead() {
  return <th>Due total</th>;
}

function PersonDue({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useSettings(sheet).data?.floor;
  const mine = live((claims.data ?? []).filter((c) => c.personId === person.id), [person]);
  return <td data-testid="person-due-total">{personDue(mine, rates.data, person.grade, today, floor)}</td>;
}

function DueTotal() {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useSettings(sheet).data?.floor;
  const dues = (people.data ?? []).map((p) =>
    personDue(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), rates.data, p.grade, today, floor),
  );
  return (
    <p>
      Due total <strong data-testid="rollup-due-total">{dueTotal(dues)}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 15, Component: DueHead }),
  PersonColumns.fill({ order: 15, Component: PersonDue }),
  HomeMain.fill({ order: 35, Component: DueTotal }),
];

import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { claimed, useClaims } from '../claims/claims';
import { due, dueTotal, rateOf, useRates } from './rates';

// A person's due-total (their claimed at their grade's rate) and the sheet's (everyone's, skipping n/a).
function DueHead() {
  return <th>Due total</th>;
}

function PersonDue({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const mine = (claims.data ?? []).filter((c) => c.personId === person.id);
  return <td data-testid="person-due-total">{due(claimed(mine), rateOf(rates.data, person.grade))}</td>;
}

function DueTotal() {
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const dues = (people.data ?? []).map((p) =>
    due(claimed((claims.data ?? []).filter((c) => c.personId === p.id)), rateOf(rates.data, p.grade)),
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

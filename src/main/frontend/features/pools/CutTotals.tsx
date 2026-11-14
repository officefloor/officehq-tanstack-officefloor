import type { Person } from '../people/people';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { trimmedTotal } from '../rates/rates';
import { cutTotal, personCuts, usePools } from './pools';

// A person's cut total: their cuts across every pool they are a member of, added up.
export function PersonCutTotal({ person }: { person: Person }) {
  const pools = usePools().data ?? [];
  return <td data-testid="person-cut-total">{cutTotal(personCuts(pools, person.id))}</td>;
}

// The sheet's cut total: everyone's cut total added up, leaving out the single highest and lowest person's figure
// when three or more have one.
export function CutTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const pools = usePools().data ?? [];

  if (!user || !sheet) return null;

  return (
    <p>
      Cut total: <strong data-testid="rollup-cut-total">{trimmedTotal(people.map((p) => cutTotal(personCuts(pools, p.id))))}</strong>
    </p>
  );
}

import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useDueRules } from '../settings/settings';
import { personDue, useRates } from './rates';
import { useColumnShown } from '../columns/columns';

// A person's due-total: the figures of their claims that count, added up; n/a when their grade has no
// rate on the sheet's today, unless a settled or stated claim keeps a figure; shared claims parted with their holders.
export function PersonDueTotal({ person }: { person: Person }) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const all = useClaims().data ?? [];
  const people = usePeople().data ?? [];
  const claims = all.filter((c) => c.personId === person.id);
  const shared = { personId: person.id, claims: all, people };
  if (!useColumnShown('due-total')) return null;
  return <td data-testid="person-due-total">{personDue(claims, rates, person.grades, liveHolds(person, rules.today), rules, shared)}</td>;
}

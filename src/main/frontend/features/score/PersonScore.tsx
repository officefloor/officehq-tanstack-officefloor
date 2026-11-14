import { useClaims } from '../claims/claims';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { personSplit, useRates } from '../rates/rates';
import { useDueRules } from '../settings/settings';
import { personOverdue } from '../overdue/overdue';
import { score } from './score';

// A person's score: their settled counting figures / 100, minus 5 per overdue counting claim, rounded half to even.
export function PersonScore({ person }: { person: Person }) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  const holds = liveHolds(person, rules.today);
  const { settled } = personSplit(claims, rates, person.grades, holds, rules);
  const { count } = personOverdue(claims, rates, person.grades, holds, rules);
  return <td data-testid="person-score">{score(settled, count)}</td>;
}

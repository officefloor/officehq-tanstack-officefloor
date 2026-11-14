import { useClaims } from '../claims/claims';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { personSplit, useRates } from '../rates/rates';
import { useDueRules, useSettings } from '../settings/settings';
import { personOverdue } from '../overdue/overdue';
import { band, score } from './score';

// A person's score: their settled counting figures / 100, minus 5 per overdue counting claim, rounded half to even.
export function PersonScore({ person }: { person: Person }) {
  return <td data-testid="person-score">{usePersonScore(person)}</td>;
}

// A person's band by the sheet's score thresholds: HIGH, MID or LOW; blank while either threshold is blank.
export function PersonBand({ person }: { person: Person }) {
  const settings = useSettings().data;
  const value = usePersonScore(person);
  return <td data-testid="person-band">{band(value, settings?.scoreMid ?? '', settings?.scoreHigh ?? '')}</td>;
}

function usePersonScore(person: Person): string {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  const holds = liveHolds(person, rules.today);
  const { settled } = personSplit(claims, rates, person.grades, holds, rules);
  const { count } = personOverdue(claims, rates, person.grades, holds, rules);
  return score(settled, count);
}

import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useSheetToday } from '../session/sheetToday';
import { useFloor, useTerms } from '../settings/settings';
import { personOverdue } from './overdue';

function usePersonOverdue(person: Person) {
  const rates = useRates().data ?? [];
  const today = useSheetToday();
  const floor = useFloor();
  const terms = useTerms();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return personOverdue(claims, rates, person.grade, person.held, floor, terms, today);
}

// How many of a person's counting claims are overdue.
export function PersonOverdueCount({ person }: { person: Person }) {
  return <td data-testid="person-overdue-count">{usePersonOverdue(person).count}</td>;
}

// The figures of a person's overdue counting claims, added up.
export function PersonOverdueTotal({ person }: { person: Person }) {
  return <td data-testid="person-overdue-total">{usePersonOverdue(person).total}</td>;
}

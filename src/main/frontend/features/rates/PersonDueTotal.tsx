import { claimed, useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { useSheetToday } from '../session/sheetToday';
import { due, rateFor, useRates } from './rates';

// A person's due-total: what they have claimed at their grade's rate in force on the sheet's today.
export function PersonDueTotal({ person }: { person: Person }) {
  const rates = useRates().data ?? [];
  const today = useSheetToday();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return <td data-testid="person-due-total">{due(claimed(claims), rateFor(rates, person.grade, today))}</td>;
}

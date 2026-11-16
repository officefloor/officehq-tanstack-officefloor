import { isOpen, live, type Claim } from '../claims/claims';
import { type Person } from '../people/people';
import { plusDays } from '../overdue/late';

/** Whether a person is active on the sheet's today (yyyy-MM-dd): they have an open (OPEN or PENDING) claim that
 *  counts in the live totals, dated within the 30 days up to and including today. */
export function isActive(person: Person, claims: Claim[] | undefined, today: string): boolean {
  if (today === '') return false;
  const since = plusDays(today, -29);
  return live(claims?.filter((c) => c.personId === person.id) ?? [], [person]).some(
    (c) => isOpen(c) && c.date >= since && c.date <= today,
  );
}

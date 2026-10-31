import { isOpen, live, type Claim } from '../claims/useClaims';
import { heldOn, type Person } from '../people/usePeople';

/** How many days back from the sheet's today a claim keeps its person active: dated within the 30
 *  days up to and including today (today and the 29 days before it). */
const ACTIVE_DAYS = 30;

/** The yyyy-mm-dd `days` days before `date`. */
function daysBefore(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) - days * 86_400_000).toISOString().slice(0, 10);
}

/** Whether a person is active on the sheet's `today`: they have an OPEN or PENDING claim that counts
 *  (see `live`) dated within the 30 days up to and including today. `claims` may be the whole sheet's. */
export function isActive(person: Person, claims: Claim[], today: string): boolean {
  if (today === '') return false;
  const since = daysBefore(today, ACTIVE_DAYS - 1);
  return live(
    claims.filter((c) => c.personId === person.id),
    (_, date) => heldOn(person, date, today),
  ).some((c) => isOpen(c) && c.date >= since && c.date <= today);
}

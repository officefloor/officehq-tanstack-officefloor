import { counts, isOpen, useClaims } from '../claims/useClaims';
import { holdsById, usePeople } from '../people/usePeople';
import { useSheetToday } from '../today/useSheetToday';

/** The first day (yyyy-mm-dd) of the 30 days up to and including a day. */
function windowStart(today: string): string {
  return new Date(Date.parse(`${today}T00:00:00Z`) - 29 * 86_400_000).toISOString().slice(0, 10);
}

/** Whether each person is active, by person id: they have an open (OPEN or PENDING) claim that counts
 *  (see `counts`) dated within the 30 days up to and including the sheet's today. */
export function useActive(sheetId: string): Map<number, boolean> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  const holds = holdsById(people.data);
  const active = new Map<number, boolean>();
  if (today === undefined) return active;
  const from = windowStart(today);
  for (const p of people.data ?? []) {
    active.set(p.id, (claims.data ?? []).some((c) =>
      c.personId === p.id && isOpen(c) && counts(c, holds, today) && c.date >= from && c.date <= today));
  }
  return active;
}

import { useQuery } from '@tanstack/react-query';
import { claimsKey, counts, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { claimDue, listRates, ratesKey, sumDues } from './api';

/** Per calendar month (yyyy-mm) any claim on the sheet is dated in, the due of the claims dated in it
 *  that count (live ones only, see `liveClaims`), n/a ones skipped; earliest month first. */
export function MonthTotals({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const charges = useLate(sheetId, today);
  const all = claims.data ?? [];
  const byId = new Map((people.data ?? []).map((p) => [p.id, p]));
  const live = liveClaims(all, people.data).filter(counts);
  const months = [...new Set(all.map((c) => c.date.slice(0, 7)))].sort();
  const dueIn = (m: string) => sumDues(live.filter((c) => c.date.slice(0, 7) === m).map((c) => {
    const p = byId.get(c.personId);
    return p ? claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges) : null;
  }));
  return (
    <section>
      <h3>Monthly due</h3>
      <ul>
        {months.map((m) => (
          <li key={m}>{m} <span data-testid={`month-${m}-due-total`}>{dueIn(m)}</span></li>
        ))}
      </ul>
    </section>
  );
}

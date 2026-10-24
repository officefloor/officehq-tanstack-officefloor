import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, usePeople } from '../people/people';
import { useSettings } from '../roles/roles';
import { chargedOf, sumDue, useRates } from '../rates/rates';
import { countsLive, holdsOf, useClaims } from './claims';

// Per calendar month (yyyy-mm, sorted) that any claim on the sheet is dated in, its due-total: the dues (with any
// late charge, as shown now, each at the grade its person had on its date) of the claims dated in it that count
// live, added up, skipping n/a.
function MonthTotals() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const today = useSheetToday();
  if (!user || !sheet) return null;
  const allClaims = claims.data ?? [];
  const personOf = new Map((people.data ?? []).map((p) => [p.id, p]));
  const held = holdsOf(people.data);
  const monthOf = (date: string) => date.slice(0, 7);
  const months = [...new Set(allClaims.map((c) => monthOf(c.date)))].sort();
  return (
    <section data-testid="month-totals">
      <h2>Months</h2>
      <table>
        <thead>
          <tr>
            <th>Month</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {months.map((m) => (
            <tr key={m}>
              <td>{m}</td>
              <td data-testid={`month-${m}-due-total`}>
                {sumDue(allClaims
                  .filter((c) => monthOf(c.date) === m && personOf.has(c.personId) && countsLive(c, held, today))
                  .map((c) => chargedOf(c, rates.data, gradeOn(personOf.get(c.personId)!, c.date), settings.data, today)))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 34.5, Component: MonthTotals });

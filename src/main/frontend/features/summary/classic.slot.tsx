import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, usePeople, type Person } from '../people/people';
import { countsLive, dueByOf, holdsOf, useClaims, type Claim } from '../claims/claims';
import { personDue } from '../rates/due.slot';
import { chargedOf, sumDue, useRates, type Rate } from '../rates/rates';
import { useSettings, type Settings } from '../roles/roles';
import { periodOn, usePeriods, type Period } from '../periods/periods';

/** A period's classic due-total: the dues (with any late charge, as shown now) of the claims whose due-by falls in it
 *  that count by the status they have now (OPEN, PENDING, SETTLED; not a person's unbound OPEN ones dated within a hold
 *  of theirs), added up, skipping n/a; for a closed period, the one fixed when it was closed. */
export function classicPeriodDue(period: Period, people: Person[] | undefined, claims: Claim[], rates: Rate[] | undefined,
  settings: Settings | undefined, today: string | undefined): string {
  if (period.closedTotal !== null) return period.closedTotal;
  const personOf = new Map((people ?? []).map((p) => [p.id, p]));
  const held = holdsOf(people);
  return sumDue(claims
    .filter((c) => countsLive(c, held, today) && personOf.has(c.personId) && periodOn([period], dueByOf(c)))
    .map((c) => chargedOf(c, rates, gradeOn(personOf.get(c.personId)!, c.date), settings, today)));
}

// The classic summary, beside the summary: per period (by from-date), its due-total counting each claim by its status
// now; and the sheet's due-total with no trimming — everyone's due-total added up, skipping n/a.
function ClassicSummary() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const periods = usePeriods();
  const settings = useSettings();
  const today = useSheetToday();
  if (!user || !sheet) return null;
  const allClaims = claims.data ?? [];
  const sortedPeriods = [...(periods.data ?? [])].sort((a, b) => (a.from < b.from ? -1 : a.from > b.from ? 1 : 0));
  const total = sumDue((people.data ?? []).map((p) => personDue(p, allClaims, rates.data, today, settings.data)));
  return (
    <section data-testid="classic-summary">
      <h2>Classic summary</h2>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {sortedPeriods.map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`classic-period-${p.name}-due-total`}>
                {classicPeriodDue(p, people.data, allClaims, rates.data, settings.data, today)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Due in total <strong data-testid="classic-due-total">{total}</strong>
      </p>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 33.5, Component: ClassicSummary });

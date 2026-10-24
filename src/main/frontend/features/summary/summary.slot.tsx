import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { personDue } from '../rates/due.slot';
import { sumDue, useRates } from '../rates/rates';
import { useSettings } from '../roles/roles';
import { periodDue, usePeriods } from '../periods/periods';

/** The statuses the summary counts claims by. */
const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

// The summary panel, figured the same way as the list: per grade, how many people not retired have it in force on
// the sheet's today and their due-totals added up (skipping n/a); per period, its due-total; per status, how
// many of the sheet's claims have it.
function Summary() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const periods = usePeriods();
  const settings = useSettings();
  const today = useSheetToday();
  if (!user || !sheet) return null;
  const active = (people.data ?? []).filter((p) => p.retiredFrom === null);
  const allClaims = claims.data ?? [];
  return (
    <section data-testid="summary">
      <h2>Summary</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = active.filter((p) => gradeOn(p, today) === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>
                  {sumDue(onGrade.map((p) => personDue(p, allClaims, rates.data, today, settings.data)))}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>
                {periodDue(p, people.data, allClaims, rates.data, settings.data, today)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Status</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{allClaims.filter((c) => c.status === s).length}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 33, Component: Summary });

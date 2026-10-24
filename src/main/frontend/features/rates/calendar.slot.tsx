import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { GRADES } from '../people/people';
import { rateOn, useRates } from './rates';

// The rate calendar: for each grade and each month of the sheet's today's calendar year, the rate in force on the
// first day of that month (as the rate in force today is found: up the sheet's parents when it has none), n/a when none.
function RateCalendar() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const rates = useRates();
  if (!user || !sheet || today === undefined) return null;
  const year = today.slice(0, 4);
  const months = Array.from({ length: 12 }, (_, i) => `${year}-${String(i + 1).padStart(2, '0')}`);
  return (
    <section data-testid="rate-calendar">
      <h2>Rate calendar {year}</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            {months.map((m) => (
              <th key={m}>{m}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              {months.map((m) => (
                <td key={m} data-testid={`rate-month-${g}-${m}`}>
                  {rateOn(rates.data, g, `${m}-01`) ?? 'n/a'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 5.2, Component: RateCalendar });

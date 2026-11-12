import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';
import { counts, live, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { contains, usePeriods } from '../periods/periods';
import { claimDue, dueTotal, useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { lateOf } from '../overdue/overdue';
import { usePersonDue } from '../rates/due.slot';
import { useFigure } from '../locale/locale';

// The classic summary, shown beside the summary: per period, its due-total counting each claim by its
// status now (OPEN, SETTLED and PENDING count, by the live rules), a closed period showing the figure fixed
// when it closed; and the sheet's due-total untrimmed, everyone's due-totals added up (skipping n/a).
function ClassicSummary() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const periods = usePeriods(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  const personDue = usePersonDue(sheet);

  const periodTotal = (period: NonNullable<typeof periods.data>[number]) => {
    if (period.closed) return period.total ?? '0.00';
    return dueTotal(
      (people.data ?? []).flatMap((p) =>
        live((claims.data ?? []).filter((c) => c.personId === p.id), [p])
          .filter((c) => counts(c) && contains(period, c.dueBy))
          .map((c) => claimDue(c, rates.data, p.grades, floor, lateOf(settings, today, [p]))),
      ),
    );
  };

  return (
    <section data-testid="classic-summary">
      <h2>Classic summary</h2>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`classic-period-${p.name}-due-total`}>{fig(periodTotal(p))}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Sheet due total: <span data-testid="classic-due-total">{fig(dueTotal((people.data ?? []).map(personDue)))}</span>
      </p>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 47, Component: ClassicSummary });

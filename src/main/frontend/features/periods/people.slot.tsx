import { Fragment } from 'react';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { personPeriodCarried, personPeriodDue, usePeriods } from './periods';
import { useFigure } from '../locale/locale';

// For every period on the sheet, a person's due-total for it: live while the period is open, fixed
// when it was closed; and the figure carried into it from earlier periods' claims still open at its end, likewise
// live while it is open and fixed when it was closed.
function PeriodHeads() {
  const [sheet] = useCurrentSheet();
  const periods = usePeriods(sheet);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <Fragment key={p.name}>
          <th>{p.name} due</th>
          <th>{p.name} carried</th>
        </Fragment>
      ))}
    </>
  );
}

function PersonPeriods({ person }: { person: Person }) {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const periods = usePeriods(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const late = lateOf(settings, today, [person]);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <Fragment key={p.name}>
          <td data-testid={`person-period-${p.name}-due-total`}>
            {fig(personPeriodDue(p, person, claims.data, rates.data, floorOf(settings), late))}
          </td>
          <td data-testid={`person-period-${p.name}-carried`}>
            {fig(personPeriodCarried(p, periods.data, person, claims.data, rates.data, floorOf(settings), late))}
          </td>
        </Fragment>
      ))}
    </>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 16, Component: PeriodHeads }),
  PersonColumns.fill({ order: 16, Component: PersonPeriods }),
];

import { Fragment } from 'react';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useFloor, useLate } from '../access/access';
import { personPeriodCarried, personPeriodDueTotal, usePeriods } from './periods';

function PersonPeriodsHead({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId).data ?? [];
  return (
    <>
      {periods.map((p) => (
        <Fragment key={p.name}>
          <th>{p.name}</th>
          <th>{p.name} carried</th>
        </Fragment>
      ))}
    </>
  );
}

// A person's due-total for each period on the sheet: live while the period is open, fixed when it
// was closed; and the figure carried into it from earlier periods.
function PersonPeriods({ sheetId, person }: { sheetId: string; person: Person }) {
  const periods = usePeriods(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return (
    <>
      {periods.map((p) => (
        <Fragment key={p.name}>
          <td data-testid={`person-period-${p.name}-due-total`}>
            {personPeriodDueTotal(p, person, claims, rates, floor, late)}
          </td>
          <td data-testid={`person-period-${p.name}-carried`}>
            {personPeriodCarried(p, periods, person, claims, rates, floor, late)}
          </td>
        </Fragment>
      ))}
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 22, Component: PersonPeriodsHead }),
  PersonRow.fill({ order: 22, Component: PersonPeriods }),
];

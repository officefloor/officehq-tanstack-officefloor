import { Fragment } from 'react';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useFloorCents } from '../settings/settings';
import { useClaims, useLate } from '../claims/claims';
import {
  personPeriodCarriedCents,
  personPeriodDueCents,
  shownPersonCarried,
  shownPersonDueTotal,
  usePeriods,
} from './periods';
import { Fig } from '../locale/Fig';

function PeriodHeaders({ sheet }: { sheet: string; user: string }) {
  const { data: periods = [] } = usePeriods(sheet);
  return (
    <>
      {periods.map((p) => (
        <Fragment key={p.name}>
          <th>{p.name} due</th>
          <th>{p.name} carried</th>
        </Fragment>
      ))}
    </>
  );
}

// Per period on the sheet, the person's due-total for it and their carried figure (see personPeriodCarriedCents): the due now of their claims whose due-by falls in it that counted at its end,
// both fixed for everyone on the sheet when the period is (soft-)closed (0.00 for a person added since) and live
// again once it is reopened. The period's own due-total is the sum of the due-totals.
function PersonPeriods({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: periods = [] } = usePeriods(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);

  const totals = personPeriodDueCents(periods, claims, people, rates, floor, late);
  const carried = personPeriodCarriedCents(periods, claims, people, rates, floor, late);

  return (
    <>
      {periods.map((p) => (
        <Fragment key={p.name}>
          <td data-testid={`person-period-${p.name}-due-total`}>
            <Fig>{shownPersonDueTotal(p, person.id, totals.get(p.name)?.get(person.id) ?? 0n)}</Fig>
          </td>
          <td data-testid={`person-period-${p.name}-carried`}>
            <Fig>{shownPersonCarried(p, person.id, carried.get(p.name)?.get(person.id) ?? 0n)}</Fig>
          </td>
        </Fragment>
      ))}
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 15, Component: PeriodHeaders }),
  PersonColumns.fill({ order: 15, Component: PersonPeriods }),
];

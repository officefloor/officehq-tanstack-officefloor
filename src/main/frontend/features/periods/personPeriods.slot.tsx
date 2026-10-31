import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useClaims } from '../claims/useClaims';
import { type Person } from '../people/usePeople';
import { showDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { personPeriodDueCents, usePeriods } from './usePeriods';

/** One header per period on the sheet. */
function PeriodColumns({ sheet }: { user: string; sheet: string }) {
  const { data: periods = [] } = usePeriods(sheet);
  return (
    <>
      {periods.map((p) => (
        <th key={p.name}>{p.name}</th>
      ))}
    </>
  );
}

/**
 * A person's due-total for every period on the sheet: the due of their counted claims dated in it,
 * or, once the period is closed, the figure fixed then (0.00 for a person added since).
 */
function PersonPeriods({ sheet, person }: { user: string; sheet: string; person: Person }) {
  const { data: periods = [] } = usePeriods(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  return (
    <>
      {periods.map((p) => (
        <td key={p.name} data-testid={`person-period-${p.name}-due-total`}>
          {showDue(personPeriodDueCents(p, person, claims, rates, floor, late))}
        </td>
      ))}
    </>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 15, Component: PeriodColumns }),
  PersonCells.fill({ order: 15, Component: PersonPeriods }),
];

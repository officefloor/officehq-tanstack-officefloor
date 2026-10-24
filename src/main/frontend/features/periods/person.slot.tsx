import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useSheetToday } from '../session/session';
import { usePeople, type Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useSettings } from '../roles/roles';
import { personPeriodDue, usePeriods } from './periods';

// For every period on the sheet, a person's due-total for it: the dues of their counted claims whose due-by falls in it; for a
// closed period, the one fixed when it was closed (0.00 for a person added since). A period's due-total is the sum
// of these.
function PeriodHeaders() {
  const periods = usePeriods();
  return <>{(periods.data ?? []).map((p) => <th key={p.id}>{p.name}</th>)}</>;
}

function PersonPeriods({ person }: { person: Person }) {
  const periods = usePeriods();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const today = useSheetToday();
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <td key={p.id} data-testid={`person-period-${p.name}-due-total`}>
          {personPeriodDue(p, person, people.data, claims.data ?? [], rates.data, settings.data, today)}
        </td>
      ))}
    </>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 17, Component: PeriodHeaders }),
  PersonCells.fill({ order: 17, Component: PersonPeriods }),
];

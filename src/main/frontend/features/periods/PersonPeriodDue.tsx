import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useDueRules } from '../settings/settings';
import { personPeriodDue, usePeriods } from './periods';

// A person's due-total for each period on the sheet, one cell per period; fixed while the period is closed.
export function PersonPeriodDue({ person }: { person: Person }) {
  const periods = usePeriods().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return (
    <>
      {periods.map((p) => (
        <td key={p.name} data-testid={`person-period-${p.name}-due-total`}>
          {personPeriodDue(p, person, claims, rates, rules)}
        </td>
      ))}
    </>
  );
}

// A head for each period's column.
export function PersonPeriodHeads() {
  const periods = usePeriods().data ?? [];
  return (
    <>
      {periods.map((p) => (
        <th key={p.name}>{p.name} due</th>
      ))}
    </>
  );
}

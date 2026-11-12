import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { personPeriodDue, usePeriods } from './periods';

// For every period on the sheet, a person's due-total for it: live while the period is open, fixed
// when it was closed.
function PeriodHeads() {
  const [sheet] = useCurrentSheet();
  const periods = usePeriods(sheet);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <th key={p.name}>{p.name} due</th>
      ))}
    </>
  );
}

function PersonPeriods({ person }: { person: Person }) {
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
        <td key={p.name} data-testid={`person-period-${p.name}-due-total`}>
          {personPeriodDue(p, person, claims.data, rates.data, floorOf(settings), late)}
        </td>
      ))}
    </>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 16, Component: PeriodHeads }),
  PersonColumns.fill({ order: 16, Component: PersonPeriods }),
];

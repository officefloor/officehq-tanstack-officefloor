import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { counts, useClaims } from '../claims/useClaims';
import { useHeld } from '../people/people.slot';
import { addDays } from '../rates/useRates';
import { useSheetToday } from '../today/today.slot';

/** Whether a person is active: they have an OPEN or PENDING claim that counts, dated within the 30 days up to and
 *  including the sheet's today. */
export function useIsActive(sheetId: string): (personId: number) => boolean {
  const today = useSheetToday(sheetId).data?.today;
  const claims = useClaims(sheetId);
  const held = useHeld(sheetId);
  const from = today === undefined ? undefined : addDays(today, -29);
  return (personId) => today !== undefined && from !== undefined && (claims.data ?? []).some((c) =>
    c.personId === personId && (c.status === 'OPEN' || c.status === 'PENDING') && counts(c, held)
    && from <= c.date && c.date <= today);
}

function ActiveHead() {
  return <th>Active</th>;
}

// Whether the person is active: an OPEN or PENDING counting claim dated within the last 30 days.
function ActiveCell({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const isActive = useIsActive(sheetId);
  return <td data-testid="person-active">{isActive(personId) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonHeads.fill({ order: 27, Component: ActiveHead }),
  PersonCells.fill({ order: 27, Component: ActiveCell }),
];

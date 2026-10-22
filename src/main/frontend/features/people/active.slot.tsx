import { PersonRow } from '../../slots/defs/personRow';
import { active, useClaims } from '../claims/claims';
import { useSheetToday } from '../sheet/sheet';
import type { Person } from './people';

/** Whether the person is active on the sheet's today (see active): yes or no. */
function PersonActive({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = useClaims(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  return <td data-testid="person-active">{active(person, claims, today) ? 'yes' : 'no'}</td>;
}

export const contribution = PersonRow.fill({ order: 12, Component: PersonActive });

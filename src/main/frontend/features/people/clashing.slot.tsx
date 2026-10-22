import { PersonRow } from '../../slots/defs/personRow';
import type { Person } from './people';

/** Whether the person's name or one of their aliases is also used, as a name or alias, on another sheet of this
 *  sheet's tree (its ancestors and its descendants): yes or no. Such a person can be edited only by renaming them
 *  to a name that does not clash. */
function PersonClashing({ person }: { sheetId: string; user: string; person: Person }) {
  return <td data-testid="person-clashing">{person.clashing ? 'yes' : 'no'}</td>;
}

export const contribution = PersonRow.fill({ order: 6, Component: PersonClashing });

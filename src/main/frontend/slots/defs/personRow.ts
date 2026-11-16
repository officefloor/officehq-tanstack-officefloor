import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/people';

/** Header cells of the people table: one `<th>` per column a feature adds to every person's row. */
export const PeopleHead = defineSlot<{ sheetId: string; user: string }>('people.head');

/** Cells of a person's row in the people table: each contribution renders its own `<td>`s, in
 *  the same order as its PeopleHead cells. */
export const PersonRow = defineSlot<{ sheetId: string; user: string; person: Person }>('person.row');

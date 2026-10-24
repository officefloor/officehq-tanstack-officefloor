import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/people';

/** The people table's extra column headers: each contributes one or more `<th>`. */
export const PeopleColumns = defineSlot('people.columns');

/**
 * The people table's extra cells for one person's row, in the same order as PeopleColumns: each
 * contributes `<td>`s. Everything inside a person's row belongs to `person-row-<id>`.
 */
export const PersonCells = defineSlot<{ person: Person }>('people.cells');

import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/usePeople';

/** Extra column headers of the people list, one `<th>` per contribution. */
export const PersonColumns = defineSlot<{ user: string; sheet: string }>('person.columns');

/**
 * Extra cells of a person's row, one `<td>` per contribution, in the same order as their
 * PersonColumns headers. Everything kept per person (claims, ...) shows here, inside the row.
 */
export const PersonCells = defineSlot<{ user: string; sheet: string; person: Person }>('person.cells');

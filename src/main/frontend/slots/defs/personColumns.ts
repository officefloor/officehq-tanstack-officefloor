import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/people';

/** Extra column headers of the people table: each contribution renders its own <th>. */
export const PersonColumnHeads = defineSlot('people.columnHeads');

/** Extra cells of a person's row, in the same order as PersonColumnHeads: each renders its own <td>. */
export const PersonColumns = defineSlot<{ person: Person }>('people.columns');

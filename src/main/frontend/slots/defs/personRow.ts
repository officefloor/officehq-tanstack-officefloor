import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/people';

/**
 * Extra cells on a person's row in the people table. A contribution renders one or more <td>s
 * (the slot is a Fragment inside the row's <tr>).
 */
export const PersonRow = defineSlot<{ sheetId: string; user: string; person: Person }>('person.row');

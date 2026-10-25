import { defineSlot } from '../Slot';

/**
 * Extra columns of the people table. A contribution renders one `<th>` in the header slot and one
 * `<td>` per person in the cells slot, at the same order, so the columns line up.
 */
export const PersonHeads = defineSlot<{ sheetId: string; user: string }>('person.heads');
export const PersonCells = defineSlot<{ sheetId: string; user: string; personId: number }>('person.cells');

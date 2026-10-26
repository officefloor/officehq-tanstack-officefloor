import { defineSlot } from '../Slot';

/**
 * The people table, column by column. A feature that shows something per person contributes
 * `Header` (a <th>, rendered in the table head) and `Cell` (a <td>, rendered in each person's row).
 */
export const PersonHeaders = defineSlot<{ sheetId: string }>('person.headers');
export const PersonCells = defineSlot<{ sheetId: string; personId: number }>('person.cells');

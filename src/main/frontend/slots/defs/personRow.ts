import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/people';

/**
 * Cells a feature adds to each person's row in the people table, after the person's own cells.
 * A contribution renders <td> elements; it pairs with a header cell in PersonHead.
 */
export const PersonRow = defineSlot<{ sheetId: string; person: Person }>('person.row');

/** Header cells of the people table, one per PersonRow cell, in the same order. */
export const PersonHead = defineSlot<{ sheetId: string }>('person.head');

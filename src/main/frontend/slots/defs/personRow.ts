import { defineSlot } from '../Slot';

/**
 * The people table. A feature that shows something per person adds a column (a heading in
 * PersonColumnHead and a <td> in PersonCells) or rows under the person's own row (PersonDetail,
 * each contribution rendering its own <tr>, spanning PERSON_ROW_SPAN columns). All of it sits
 * inside the person's `person-row-<id>` element.
 */
export const PersonColumnHead = defineSlot<{ sheetId: string }>('person.columnHead');
export const PersonCells = defineSlot<{ sheetId: string; personId: number }>('person.cells');
export const PersonDetail = defineSlot<{ sheetId: string; personId: number }>('person.detail');

/** colSpan for a PersonDetail row: wide enough for every column. */
export const PERSON_ROW_SPAN = 100;

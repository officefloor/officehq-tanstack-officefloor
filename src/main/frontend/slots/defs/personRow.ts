import { defineSlot } from '../Slot';

/** Extra column headers of the people table, one per contributed column. */
export const PersonColumns = defineSlot('person.columns');

/** Extra cells of a person's row (in the same order as their PersonColumns headers). */
export const PersonCells = defineSlot<{ personId: number }>('person.cells');

import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/people';

/** Extra header cells of the people table, one per column a feature adds. */
export const PersonColumnHeads = defineSlot('person.columnHeads');

/** Extra cells of a person's row, in the same order as their header cells. */
export const PersonColumns = defineSlot<{ person: Person }>('person.columns');

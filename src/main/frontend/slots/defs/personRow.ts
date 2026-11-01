import { defineSlot } from '../Slot';
import type { Person } from '../../features/people/people';

/**
 * Extra columns of the people table. A column fills both regions: its header cell in
 * PersonColumnHeaders and, per person, its <td> in PersonColumns (inside `person-row-<id>`).
 */
export const PersonColumnHeaders = defineSlot<{ sheet: string; user: string }>('person.column-headers');
export const PersonColumns = defineSlot<{ sheet: string; user: string; person: Person }>('person.columns');

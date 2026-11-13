import { defineSlot } from '../Slot';
import type { Claim } from '../../features/claims/claims';
import type { Person } from '../../features/people/people';

/** Extra values shown in a claim's row, after its note: each contribution renders its own inline element. */
export const ClaimRowValues = defineSlot<{ claim: Claim; person: Person }>('claims.rowValues');

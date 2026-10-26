import { defineSlot } from '../Slot';
import type { Claim } from '../../features/claims/claimsQuery';

/**
 * A person's claims table, column by column. A feature that shows something per claim contributes
 * a <td>, rendered in each claim's row after its own cells.
 */
export const ClaimCells = defineSlot<{ sheetId: string; claim: Claim }>('claim.cells');

import { defineSlot } from '../Slot';

/**
 * Extra cells of a claim's row, after its amount, date and note. The context is the claim itself
 * and the person it belongs to.
 */
export const ClaimCells = defineSlot<{
  sheetId: string;
  user: string;
  personId: number;
  claim: { id: number; personId: number; amount: string; date: string; note: string; status: 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';
    settledDue: string | null; openedBy: string | null; stated: boolean; statedDue: string | null };
}>('claim.cells');

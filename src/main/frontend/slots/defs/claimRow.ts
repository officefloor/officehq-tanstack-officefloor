import { defineSlot } from '../Slot';

/**
 * Extra cells of a claim's row, after its amount, date and note. The context is the claim itself
 * and the person it belongs to.
 */
export const ClaimCells = defineSlot<{
  sheetId: string;
  user: string;
  personId: number;
  claim: { id: number; personId: number; amount: string; date: string; note: string; status: 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';
    settledDue: string | null; openedBy: string | null; stated: boolean; statedDue: string | null; dueBy: string; log: string[];
    deferred?: boolean; deferredUntil?: string | null; proofs?: number; splitFrom?: string | null; lateEntry?: boolean;
    reminded?: string | null; corrects?: string | null; ref?: string | null; tags?: string[] };
}>('claim.cells');

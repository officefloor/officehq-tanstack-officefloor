import { isDeferred, isOpen, type Claim } from './useClaims';

/** A claim's due-by: fixed when it was created, from the terms of its person's grade in force on its date. */
export const dueBy = (c: Claim) => c.dueBy;

/** A claim is overdue when it is OPEN (or PENDING), not deferred, and its due-by is before the sheet's today. */
export const isOverdue = (c: Claim, today: string | undefined) =>
  isOpen(c) && !isDeferred(c, today) && today !== undefined && dueBy(c) < today;

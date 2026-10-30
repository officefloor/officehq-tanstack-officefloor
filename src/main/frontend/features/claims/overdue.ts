import { isDeferred, isOpen, type Claim } from './useClaims';
import type { Grace } from './useGrace';

/** A claim's due-by: fixed when it was created, from the terms of its person's grade in force on its date. */
export const dueBy = (c: Claim) => c.dueBy;

/** The date a claim's lateness counts from: its due-by plus its grace days (see useGrace). */
export function lateFrom(c: { dueBy?: string; personId?: string; date: string }, grace?: Grace): string {
  const days = grace === undefined ? 0 : grace(c);
  if (!c.dueBy || days === 0) return c.dueBy ?? '';
  const d = new Date(`${c.dueBy}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** A claim is overdue when it is OPEN (or PENDING), not deferred, and its due-by plus its grace days is before the sheet's today. */
export const isOverdue = (c: Claim, today: string | undefined, grace?: Grace) =>
  isOpen(c) && !isDeferred(c, today) && today !== undefined && lateFrom(c, grace) < today;

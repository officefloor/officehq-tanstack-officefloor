import { isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';

/** A claim's due-by (yyyy-MM-dd): its date plus the terms in days. */
export function dueBy(date: string, terms: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + terms);
  return d.toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN (or PENDING) and its due-by is before the sheet's today. */
export function overdue(claim: Claim, terms: number, today: string): boolean {
  return isOpen(claim) && today !== '' && dueBy(claim.date, terms) < today;
}

/** How many full 30-day periods a claim is overdue on the sheet's today: 0 unless it is overdue. */
export function latePeriods(claim: Claim, terms: number, today: string): number {
  if (!overdue(claim, terms, today)) return 0;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${dueBy(claim.date, terms)}T00:00:00Z`)) / 86_400_000;
  return Math.floor(Math.round(days) / 30);
}

import type { Claim } from './useClaims';

/** A claim's review day: the sheet's today when it last went to REVIEW (its latest REVIEW log line), "" if never. */
export function reviewDay(c: Claim): string {
  const line = [...(c.log ?? [])].reverse().find((l) => l.startsWith('REVIEW by '));
  return line?.match(/ on (\d{4}-\d{2}-\d{2}) /)?.[1] ?? '';
}

/** A claim is review-late when it is REVIEW, the sheet has a review SLA ("" for none) and its review day plus the
 *  SLA days is before the sheet's today. */
export function isReviewLate(c: Claim, today: string | undefined, sla: string | undefined): boolean {
  const day = reviewDay(c);
  if (c.status !== 'REVIEW' || today === undefined || !sla || day === '') return false;
  const d = new Date(`${day}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + Number(sla));
  return d.toISOString().slice(0, 10) < today;
}

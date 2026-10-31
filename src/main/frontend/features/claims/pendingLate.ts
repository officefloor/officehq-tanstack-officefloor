import type { Claim } from './useClaims';

/** A claim's pending day: the sheet's today when it last went to PENDING (its latest PENDING log line), "" if never. */
export function pendingDay(c: Claim): string {
  const line = [...(c.log ?? [])].reverse().find((l) => l.startsWith('PENDING by '));
  return line?.match(/ on (\d{4}-\d{2}-\d{2}) /)?.[1] ?? '';
}

/** A claim is pending-late when it is PENDING, the sheet has a pending SLA ("" for none) and its pending day plus the
 *  SLA days is before the sheet's today. */
export function isPendingLate(c: Claim, today: string | undefined, sla: string | undefined): boolean {
  const day = pendingDay(c);
  if (c.status !== 'PENDING' || today === undefined || !sla || day === '') return false;
  const d = new Date(`${day}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + Number(sla));
  return d.toISOString().slice(0, 10) < today;
}

import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** A claim; `amount` is shown with two decimals ("10.25"); `number` is the one given when it was
 *  opened (a voided claim keeps it), "" for a draft; `due` is the due fixed when it was settled, else the
 *  figure the person's last statement bound it to, "" while its due is live (worked from the current
 *  rate) or bound to n/a (a PENDING claim is never bound: its due is live); `stated` is whether the last statement bound it. A claim with a negative
 *  amount is a correction: its due has no floor and no late charge. */
export type Claim = {
  id: string;
  personId: string;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string;
  due: string;
  /** The user who opened the claim, "" if it was never opened. */
  openedBy: string;
  stated: boolean;
  /** The number of the SETTLED claim a correction (a negative amount) corrects, "" for any other claim. */
  corrects: string;
  /** The claim's external reference, unique on the sheet regardless of case; "" for none. */
  ref: string;
  /** The claim's tags, lower-cased, sorted and comma separated ("a,b"); "" for none. */
  tags?: string;
  /** The date the claim's rounding goes by: a correction's is the date of the claim it corrects. */
  roundingDate: string;
  /** The claim's due-by, fixed when it was created: its date plus the first of its person's own terms, their grade's terms and the sheet's terms in force on that date. */
  dueBy: string;
  /** The claim's status log, oldest first: "<STATUS> by <user> on <date> due=<due>" per status it has had. */
  log: string[];
  /** The date the claim was last deferred until, "" if it was never deferred. */
  deferredUntil: string;
  /** The claim's proofs count. */
  proofs: string;
  /** The number of the claim a split made this claim from, "" for any other claim. */
  splitFrom: string;
  /** The user whose open editor locks the claim for everyone else, "" when unlocked. */
  lockedBy?: string;
  /** Whether the claim was added while its period was soft-closed. */
  lateEntry?: boolean;
  /** The last day (the sheet's today then) a reminder was written for the claim, "" if never. */
  remindedOn?: string;
  /** The reason the claim was voided with, from the sheet's list; "" for none. */
  voidReason?: string;
};

/** OPEN and PENDING claims are open: their due is live (unless bound), they go overdue and a hold
 *  takes them out of the totals. */
export const isOpen = (c: { status?: string }) => c.status === 'OPEN' || c.status === 'PENDING';

/** Only OPEN, PENDING and SETTLED claims count toward claimed, due-total and every total. */
export const counts = (c: Claim) => isOpen(c) || c.status === 'SETTLED';

/** Whether a statement binds the claim's figure now: a PENDING claim is never bound. */
export const isBound = (c: Claim) => c.stated && c.status !== 'PENDING';

/** Whether a claim is deferred on the sheet's today: OPEN and deferred until a later date. A deferred claim
 *  counts in no total, is not overdue, cannot be settled and is left unbound by a statement. */
export const isDeferred = (c: { status?: string; deferredUntil?: string }, today: string | undefined) =>
  c.status === 'OPEN' && !!c.deferredUntil && today !== undefined && today < c.deferredUntil;

/** Whether a claim counts in the live totals: it counts, is not deferred on the sheet's today, and is not
 *  open, unbound and dated within one of its person's holds (`heldOn`). */
export const countsLive = (c: Claim, heldOn: (date: string) => boolean, today: string | undefined) =>
  counts(c) && !isDeferred(c, today) && !(isOpen(c) && !isBound(c) && heldOn(c.date));

/** The status a claim had at the end of the day, as its status log records it (each line dated the sheet's today
 *  when the change was made); undefined when it was added after that day. */
export function statusAt(c: Claim, end: string): ClaimStatus | undefined {
  let at: ClaimStatus | undefined;
  for (const line of c.log ?? []) {
    const m = / on (\d{4}-\d{2}-\d{2}) due=[^ ]*$/.exec(line);
    if (m && m[1] <= end) at = line.slice(0, line.indexOf(' ')) as ClaimStatus;
  }
  return at;
}

/** Whether a claim counts in a period ending on `end`: by the status it had at the end of that day, and, as in the
 *  live totals, not deferred on the sheet's today and not open, unbound and dated within one of its person's holds. */
export const countsAtEnd = (c: Claim, end: string, heldOn: (date: string) => boolean, today: string | undefined) => {
  const then = statusAt(c, end);
  return then !== undefined && counts({ ...c, status: then }) && !isDeferred(c, today) && !(isOpen(c) && !isBound(c) && heldOn(c.date));
};

export const claimsKey = (sheet: string, asOf = '') => ['sheet', sheet, 'claims', asOf] as const;

/** The sheet's claims, newest first. */
export function useClaims(sheet: string): Claim[] {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: claimsKey(sheet, asOf),
    queryFn: () => getJson<Claim[]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/claims`, asOf)),
    enabled: sheet !== '',
  });
  return data ?? [];
}

/** The amounts of the claims that count added up, shown with two decimals. Summed in whole cents
 *  so no float drift shows. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** A claim's tags as shown: sorted, comma and space separated. */
export const tagList = (tags: string | undefined) => (tags ?? '').split(',').filter((t) => t !== '').join(', ');

/** Whether a claim carries the tag. */
export const hasTag = (c: { tags?: string }, tag: string) => (c.tags ?? '').split(',').includes(tag);

export const tagsKey = (sheet: string) => ['sheet', sheet, 'tags'] as const;

/** Every tag ever used on the sheet's claims, sorted. */
export function useTags(sheet: string): string[] {
  const { data } = useQuery({
    queryKey: tagsKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/tags`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

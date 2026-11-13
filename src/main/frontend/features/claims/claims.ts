import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';
import { asNumber, asString, useSearchParam } from '../../url/useSearchParam';

/** PENDING: settled by a clerk above the clerk limit, awaiting a steward's approval; handled as OPEN
 *  (a statement binds it like any other claim that counts). DELETED: a deleted DRAFT, which counts
 *  nowhere and can be restored to a DRAFT on the sheet's today it was deleted. REVIEW: opened while its due was
 *  above the review threshold on its date, awaiting a steward's review to make it OPEN; it counts in no total. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED' | 'REVIEW';

/** Every status the summary and header count, whether or not a claim can reach it; a DELETED claim counts nowhere. */
export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'] as const satisfies readonly ClaimStatus[];

/** How many of these claims have the status. */
export const statusCount = (claims: Claim[] | undefined, status: ClaimStatus) =>
  (claims ?? []).filter((c) => c.status === status).length;

/** `amount` has two decimals; `date` is yyyy-MM-dd; `number` is given when the claim is opened
 *  (null for a draft or a voided claim): the sheet-wide 1, 2, ... or, numbered per period, <period>-<n> / X-<n>. */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string | null;
  /** The due fixed when the claim was settled; null when it has none, so its due follows the rate. */
  due: string | null;
  /** The user who opened the claim; null until it is opened. */
  openedBy: string | null;
  /** Whether the person's last statement bound the claim to its figure then. */
  stated: boolean;
  /** The due as shown (late charge and floor included) the claim was bound to; null when not bound, or bound while it had none. */
  statedDue: string | null;
  /** Whether the statement bound the claim to n/a: the rate that would apply to it had ended (its to-date). */
  statedNa: boolean;
  /** The number of the settled claim a correction (a negative amount) corrects; null for any other claim. */
  corrects: string | null;
  /** The date (yyyy-MM-dd) its live figures round by: its own, or for a correction that of the claim it corrects. */
  roundsBy: string;
  /** The date (yyyy-MM-dd) it is due by, fixed when it was added as the terms-th business day after its date. */
  dueBy: string;
  /** Its status log, oldest first: `<STATUS> by <user> on <date> due=<due>`, one line per status it has had. */
  log: string[];
  /** Whether it is deferred on the sheet's today: OPEN and deferred until a date after it. Until then it
   *  counts in no total, is not overdue, cannot be settled and is left unbound by a statement. A VOID claim
   *  deferred until after it shows as deferred too, though it counts nowhere. */
  deferred: boolean;
  /** The date (yyyy-MM-dd) it was last deferred until; null when it was never deferred. */
  deferredUntil: string | null;
  /** How many proofs it has: a claim due above the proof threshold on its date cannot be settled or approved with none. */
  proofs: number;
  /** How many of its proofs are of each kind (receipt, photo), by kind; a proof added with no kind is a receipt. */
  proofKinds: Record<string, number>;
  /** The number of the claim it was split off; null when it was not split off another. */
  splitFrom: string | null;
  /** Whether it was added while the period its due-by falls in was soft-closed. */
  lateEntry: boolean;
  /** The date (yyyy-MM-dd) a reminder was last written for it; null when it was never reminded. */
  remindedOn: string | null;
  /** Its external reference, unique on the sheet without regard to case; null when it has none. */
  ref: string | null;
  /** Its tags, each trimmed and lower-cased, sorted; empty when it has none. */
  tags: string[];
  /** The reason it was voided with, from the sheet's list then; null when none. */
  voidReason: string | null;
  /** Its comments, oldest first: `<user> on <date>: <text>`, the date the sheet's today when it was written. */
  comments: string[];
  /** The bulk settle batch it was settled in (1, 2, ... on the sheet); null when settled one at a time, or not settled. */
  batch: number | null;
  /** Its lines, in the order added: `<label> <qty> x <unit>`; a claim with lines has their sum as its amount. */
  lines: string[];
  /** When it was added, in the order forward-only rates are saved in (see `Rate`); a copy keeps its original's. */
  added: number;
  /** The date (yyyy-MM-dd) it went to REVIEW on, the sheet's today then (its review day); null when it never went to REVIEW. */
  reviewOn: string | null;
  /** The date (yyyy-MM-dd) it last went to PENDING on, the sheet's today then (its pending day); null when it never went
   *  to PENDING. */
  pendingOn: string | null;
  /** Whether a steward waived its late charge: it bears none from then on, whatever its overdue state. */
  waived: boolean;
  /** Whether it is held. A held claim still counts in every total. */
  held: boolean;
  /** The people it is shared with (by id) and their share percents (two decimals), in the order shared; empty when none.
   *  Each holder's figures count their part of its due (see `sharedParts`), its person's the due less the parts. */
  shares: { personId: number; share: string }[];
  /** The user a steward assigned its review to; null when never assigned. */
  reviewUser: string | null;
};

/** A correction: a claim with a negative amount, which has no floor and no late charge. */
export const isCorrection = (c: Claim) => c.amount.trim().startsWith('-') && Number(c.amount) !== 0;

/** Whether the claim is open: OPEN, or PENDING, which is handled as open. */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/** Only OPEN, PENDING and SETTLED claims count toward claimed and the totals; a deferred claim does not. */
export const counts = (c: Claim) => (isOpen(c) && !c.deferred) || c.status === 'SETTLED';

/** Whether the claim keeps the figure (or n/a) its person's last statement bound it to. */
export const isBound = (c: Claim) => c.stated && (c.statedDue !== null || c.statedNa);

/** These claims less the OPEN (or PENDING) claims of a person whose retirement date the sheet's today has
 *  reached, which count in no live due total, unless a statement bound them. A hold (the claim's or its
 *  person's) leaves a claim in every total: it only takes the person's cuts from their pools. */
export const live = (claims: Claim[], people: { id: number; retiredNow: boolean }[] | undefined) => {
  const byId = new Map((people ?? []).map((p) => [p.id, p]));
  return claims.filter((c) => {
    const p = byId.get(c.personId);
    return !(isOpen(c) && !c.stated && p !== undefined && p.retiredNow);
  });
};

/** The claims that count toward claimed: as {@link live}. */
export const liveClaimed = live;

/** The date (yyyy-MM-dd) the given number of days before this one. */
const daysBefore = (date: string, days: number) => {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - days);
  return d.toISOString().slice(0, 10);
};

/** Whether a person is active on the date (yyyy-MM-dd): one of their claims (already filtered to those
 *  live) is OPEN or PENDING, counts, and is dated within the 30 days up to and including the date. */
export const isActive = (claims: Claim[], today: string | undefined) =>
  today !== undefined &&
  claims.some((c) => isOpen(c) && counts(c) && c.date > daysBefore(today, 30) && c.date <= today);

/** Whether the claim is SETTLED and dated within the 90 days up to and including the date (yyyy-MM-dd). */
export const isRecentSettled = (c: Claim, today: string | undefined) =>
  today !== undefined && c.status === 'SETTLED' && c.date > daysBefore(today, 90) && c.date <= today;

/** Under the sheet's key, so loading the sheet again re-reads its claims. */
export const claimsKey = (sheet: string) => [...sheetKey(sheet), 'claims'] as const;

/** Under the claims' key, so whatever re-reads the claims re-reads the tags too. */
export const tagsKey = (sheet: string) => [...claimsKey(sheet), 'tags'] as const;

/** Every tag ever carried by a claim on the sheet, sorted. */
export function useTags(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(tagsKey(sheet), asOf),
    queryFn: () => getJson<string[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/tags`, asOf)),
    enabled: sheet !== '',
  });
}

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(claimsKey(sheet), asOf),
    queryFn: () => getJson<Claim[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/claims`, asOf)),
    enabled: sheet !== '',
  });
}

/** Amounts are added in whole cents, so the sum is exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

/** Whether the claim corrects another: it names the number of the claim it corrects. */
export const correctsAnother = (c: Claim) => c.corrects !== null && c.corrects.trim() !== '';

/** The sum of the amounts of those of these claims that count, leaving out claims that correct another,
 *  with two decimals. */
export function claimed(claims: Claim[]): string {
  return (claims.filter((c) => counts(c) && !correctsAnother(c)).reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}

/** The ids of the claims selected for a bulk action, kept in the URL (`selectedClaims`, comma separated). */
export function useSelectedClaims(): [number[], (ids: number[]) => void] {
  const [raw, setRaw] = useSearchParam('selectedClaims', asString);
  const ids = raw.split(',').filter((s) => s !== '').map(Number).filter((n) => !Number.isNaN(n));
  return [ids, (next) => setRaw(next.join(','))];
}

/** The id of the claim whose editor is open, and locked by this user, kept in the URL (`editClaim`). */
export const useEditingClaim = () => useSearchParam('editClaim', asNumber);

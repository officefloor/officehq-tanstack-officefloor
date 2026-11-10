import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useReader } from '../../api/readNow';

/** A grade's rate: a percent with two decimals, in force from its from-date (yyyy-mm-dd; null when it
 *  applies from the beginning of time) through its to-date (null when it has no end), and its `version`: how many versions the grade's rate from that
 *  from-date has had (this is the newest, the one in force). `level` is the sheet it
 *  is set on: 0 this sheet, 1 its parent, 2 the parent's parent, 3 the one above that. `after`: for a forward-only
 *  rate (saved with a from-date on or after the sheet's today), the claim-added number it was saved at — it applies
 *  only to claims added after it (see `Claim.added`); null when it applies to every claim. `superseded`: an older
 *  version, kept only for the claims a newer forward-only version does not apply to (never shown, never in force). */
export type Rate = {
  grade: string; pct: string; from: string | null; to: string | null; version?: number; level?: number;
  after?: number | null; superseded?: boolean;
};

export function ratesKey(sheetId: string) {
  return ['sheet', sheetId, 'rates'];
}

/** The sheet's rates (level 0), by grade then from-date (an undated rate first); a grade with no rate is not
 *  listed. Then those of the sheets its grades fall back to, nearest first (level 1 its parent, up to 3). Then the
 *  older (superseded) versions of all of them, for the claims they still apply to. */
export function useRates(sheetId: string) {
  const read = useReader();
  return useQuery({
    queryKey: read.key(ratesKey(sheetId)),
    queryFn: async () => {
      const got = await read.get<{ rates: Rate[]; inherited: Rate[][]; earlier: Rate[]; inheritedEarlier: Rate[][] }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/rates`);
      return [
        ...got.rates.map((r) => ({ ...r, level: 0 })),
        ...(got.inherited ?? []).flatMap((rates, i) => rates.map((r) => ({ ...r, level: i + 1 }))),
        ...(got.earlier ?? []).map((r) => ({ ...r, level: 0, superseded: true })),
        ...(got.inheritedEarlier ?? []).flatMap((rates, i) => rates.map((r) => ({ ...r, level: i + 1, superseded: true }))),
      ];
    },
  });
}

/** The rate (a two-decimal percent) for a grade in force on a date (yyyy-mm-dd): on this sheet, the one with
 *  the latest from-date on or before it, an undated rate applying from the beginning of time, unless the date is
 *  after that rate's to-date; when this sheet has none in force, its parent's the same way, then the parent's
 *  parent's, up to three levels up; undefined when none applies.
 *
 *  Given a claim's added number (`added`, see `Claim.added`), the rate that claim takes: the same way, among only
 *  the rate versions that apply to it — those that apply to every claim and the forward-only ones saved before it
 *  was added — the newest of each from-date. Without one, the rate in force, whatever claim it is for. */
export function rateFor(rates: Rate[] | undefined, grade: string | undefined, on: string | undefined,
  added?: number): string | undefined {
  if (on === undefined) return undefined;
  for (let level = 0; level <= 3; level++) {
    let found: Rate | undefined;
    for (const r of rates ?? []) {
      if ((r.level ?? 0) !== level || r.grade !== grade || (r.from !== null && r.from > on)) continue;
      if (added === undefined ? r.superseded : r.after != null && r.after >= added) continue;
      if (found === undefined || (found.from ?? '') < (r.from ?? '')
        || ((found.from ?? '') === (r.from ?? '') && (found.version ?? 0) < (r.version ?? 0))) found = r;
    }
    if (found !== undefined && (found.to == null || found.to >= on)) return found.pct;
  }
  return undefined;
}

/** A two-decimal number as a whole count of hundredths ("12.50" -> 1250, "-0.30" -> -30). */
export function hundredths(v: string): bigint {
  const t = v.trim();
  const negative = t.startsWith('-');
  const [whole, frac = ''] = (negative ? t.slice(1) : t).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

/** A whole count of hundredths as a two-decimal number (-30 -> "-0.30"). */
export function money(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/** A rate figure: an amount (two decimals) times a rate (a two-decimal percent), divided by 100, rounded
 *  half up (when `halfUp`) or half to even to two decimals — not floored (see `atLeast`); 'n/a' when there is
 *  no rate. Worked in whole numbers, so exact. */
export function due(amount: string, pct: string | undefined, halfUp: boolean): string {
  if (pct === undefined) return 'n/a';
  return money(percentOf(amount, pct, 1n, halfUp));
}

/** A holder's part of a claim: its amount (two decimals) times a share (a two-decimal percent) divided by 100, times
 *  a rate (a two-decimal percent) divided by 100, rounded half up (when `halfUp`, away from zero) or half to even to
 *  two decimals; 'n/a' when there is no rate. Worked in whole numbers, so exact. */
export function heldPart(amount: string, share: string, pct: string | undefined, halfUp: boolean): string {
  if (pct === undefined) return 'n/a';
  const signed = hundredths(amount) * hundredths(share) * hundredths(pct); // in 1/1_000_000 of a unit, before the two divisions by 100
  const product = signed < 0n ? -signed : signed;
  const rest = product % 100000000n;
  let cents = product / 100000000n;
  if (rest > 50000000n || (rest === 50000000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return money(signed < 0n ? -cents : cents);
}

/** A figure (two decimals) raised to at least a floor (two decimals; none when undefined); 'n/a' stays 'n/a'. */
export function atLeast(figure: string, floor: string | undefined): string {
  if (figure === 'n/a' || floor === undefined || hundredths(figure) >= hundredths(floor)) return figure;
  return money(hundredths(floor));
}

/** Whether an amount (two decimals) is a correction's: below zero. */
export function isCorrection(amount: string): boolean {
  return hundredths(amount) < 0n;
}

/** An amount (two decimals) times a rate (a two-decimal percent) times a whole number, divided by
 *  100, rounded half up (when `halfUp`, away from zero) or half to even, in hundredths (a negative amount rounds as its size does, then takes
 *  its sign back). */
function percentOf(amount: string, pct: string, times: bigint, halfUp: boolean): bigint {
  const signed = hundredths(amount) * hundredths(pct) * times; // in 1/1_000_000 of a unit
  const product = signed < 0n ? -signed : signed;
  const rest = product % 10000n;
  let cents = product / 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return signed < 0n ? -cents : cents;
}

/** A figure (two decimals) with a late charge added on top: for each full 30 days, the late percent
 *  (two decimals) in force on that boundary of the figure plus the charges of the boundaries before it (compounding), each rounded like a due, the charges added up;
 *  'n/a' stays 'n/a'. */
export function withCharge(figure: string, latePcts: string[], halfUp: boolean): string {
  if (figure === 'n/a') return figure;
  return money(latePcts.reduce((sum, pct) => sum + percentOf(money(sum), pct, 1n, halfUp), hundredths(figure)));
}

/** Dues added up, skipping 'n/a'. */
export function sumDues(dues: string[]): string {
  return money(dues.filter((d) => d !== 'n/a').reduce((sum, d) => sum + hundredths(d), 0n));
}

/** A sheet's total of people's figures: added up, skipping 'n/a', but leaving out the single highest
 *  and the single lowest figure when three or more people have one. */
export function trimmedSum(figures: string[]): string {
  const cents = figures.filter((d) => d !== 'n/a').map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = cents.length >= 3 ? cents.slice(1, -1) : cents;
  return money(kept.reduce((sum, c) => sum + c, 0n));
}

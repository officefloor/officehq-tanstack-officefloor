import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useView, viewKey, viewPath } from '../asof/useAsOf';

/** A grade's rate as the server sends it: `pct` is a percent with two decimals, `from` the date it
 *  is in force from (yyyy-mm-dd), or null when it applies from the beginning of time; `to` the
 *  date it applies through, or null when it has no end; `version` how many values have been set for
 *  its grade and from-date, this newest one (the one in force) included. */
export type Rate = {
  grade: string;
  pct: string;
  from: string | null;
  to: string | null;
  version: number;
  /** Set for a forward-only rate (saved with a from-date on or after the sheet's today): it applies only to
   *  claims whose `seq` is higher. Null when it applies to every claim. */
  forwardAfter: number | null;
  /** Its grade and from-date's older versions, newest first: a claim a forward-only version does not apply
   *  to takes the newest of them that does. */
  earlier: RateVersion[];
  level?: number;
};

/** An older version of a rate, as it was set. */
type RateVersion = { pct: string; to: string | null; forwardAfter: number | null };

/** The rates of one of the sheets the sheet's rates fall back to, `level` places along (1 for the nearest). */
type InheritedRates = { level: number; sheet: string; rates: Rate[] };

export const ratesKey = (sheet: string) => ['sheet', sheet, 'rates'] as const;

/** The sheet's own rates, by grade then from-date (the one with none first), each with `level` 0;
 *  then the rates of the sheets it falls back to, nearest first, each with the level it is from: its
 *  parent sheets up to three levels up, then, for a cloned sheet, the sheet it was cloned from and
 *  that one's own parents and source. Re-read whenever the sheet is loaded. */
export function useRates(sheet: string) {
  const view = useView();
  return useQuery({
    queryKey: viewKey(ratesKey(sheet), view),
    queryFn: async () => {
      const base = `/api/sheets/${encodeURIComponent(sheet)}`;
      const [own, inherited] = await Promise.all([
        getJson<Rate[]>(viewPath(`${base}/rates`, view)),
        getJson<InheritedRates[]>(viewPath(`${base}/rates/inherited`, view)),
      ]);
      return [
        ...own.map((r) => ({ ...r, level: 0 })),
        ...inherited.flatMap((i) => i.rates.map((r) => ({ ...r, level: i.level }))),
      ];
    },
    staleTime: 0,
    gcTime: 0,
  });
}

/** A percent the server accepts: 0.00 or more, with up to two decimals. */
export const PCT = /^\d{1,5}(\.\d{1,2})?$/;

/**
 * The grade's rate in force on `date` (yyyy-mm-dd): the sheet's own (level 0) when it has one then,
 * else its parent's then, and so on up the levels.
 */
export function rateOn(rates: Rate[], grade: string, date: string): string | undefined {
  const levels = [...new Set(rates.map((r) => r.level ?? 0))].sort((a, b) => a - b);
  for (const level of levels) {
    const pct = levelRateOn(rates.filter((r) => (r.level ?? 0) === level), grade, date);
    if (pct !== undefined) return pct;
  }
  return undefined;
}

/**
 * The grade's rate in force on `date` among `rates` (of one sheet): the one whose from-date is the latest on or
 * before it, a rate with no from-date applying from the beginning of time. Undefined when none does, or when that one's to-date is before `date`.
 */
function levelRateOn(rates: Rate[], grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found !== undefined && (found.to === null || found.to >= date) ? found.pct : undefined;
}

/**
 * The grade's rate that applies to a claim on its date: as `rateOn`, but passing over each forward-only
 * rate saved after the claim was added — for the newest older version of its from-date that applies to
 * the claim, else for the from-date before. The rates panel and the preview's candidate go by `rateOn`.
 */
export function claimRateOn(rates: Rate[], grade: string, claim: { date: string; seq: number }): string | undefined {
  const applies = (v: { forwardAfter: number | null }) => v.forwardAfter === null || claim.seq > v.forwardAfter;
  const applying: Rate[] = [];
  for (const r of rates) {
    const v = applies(r) ? r : r.earlier.find(applies);
    if (v !== undefined) applying.push({ ...r, pct: v.pct, to: v.to, forwardAfter: v.forwardAfter });
  }
  return rateOn(applying, grade, claim.date);
}

/** "12.34" as a whole number of hundredths (1234n), exactly. */
export function hundredths(value: string): bigint {
  const negative = value.startsWith('-');
  const [whole, frac = ''] = (negative ? value.slice(1) : value).split('.');
  const cents = BigInt(whole) * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

function format(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/**
 * What is due on `amount` (two decimals) at `pct` percent: amount times pct divided by 100,
 * rounded to two decimals (half up with `halfUp`, else half to even), worked in whole numbers so it
 * is exact. In cents, or null when there is no rate.
 */
export function dueCents(amount: string, pct: string | undefined, halfUp = false): bigint | null {
  if (pct === undefined) return null;
  // cents * hundredths-of-a-percent is in ten-thousandths of a cent.
  // Rounded on its size, so a negative amount (a correction) rounds like a positive one.
  const product = hundredths(amount) * hundredths(pct);
  const abs = product < 0n ? -product : product;
  const rounded = roundTenThousandths(abs, halfUp);
  return product < 0n ? -rounded : rounded;
}

/** A size in ten-thousandths of a cent, rounded to whole cents: half up with `halfUp`, else half to even.
 *  `beyond` is any finer remainder below the ten-thousandths (more than 0 breaks a half upwards). */
export function roundTenThousandths(abs: bigint, halfUp: boolean, beyond = 0n): bigint {
  const whole = abs / 10000n;
  const rest = abs % 10000n;
  return rest > 5000n || (rest === 5000n && (beyond > 0n || halfUp || whole % 2n === 1n)) ? whole + 1n : whole;
}

/** A due in cents as shown: two decimals, or n/a when there is no rate. */
export function showDue(cents: bigint | null): string {
  return cents === null ? 'n/a' : format(cents);
}

/** Dues added up, skipping the n/a ones. */
export function totalDue(dues: Array<bigint | null>): string {
  return format(dues.reduce<bigint>((sum, d) => (d === null ? sum : sum + d), 0n));
}

/** The figures (n/a skipped) added up, leaving out the single highest and lowest when three or more. */
export function trimmedTotal(dues: Array<bigint | null>): string {
  const figures = dues.filter((d): d is bigint => d !== null).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  return totalDue(figures.length >= 3 ? figures.slice(1, -1) : figures);
}

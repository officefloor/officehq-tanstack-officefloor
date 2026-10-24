import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { dueByOf, isBound, isOverdue, type Claim } from '../claims/claims';
import { settingOn, type Settings } from '../roles/roles';
import { useCurrentSheet } from '../session/session';

/** A grade's rate as the store holds it: the percent with two decimals, from a date on (YYYY-MM-DD; null
 *  when it applies from the beginning of time), through a to-date (null when it has no end). */
export type Rate = { grade: string; pct: string; from: string | null; to: string | null };

export const ratesQueryKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

export const ratesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rates`;

/** The sheet's rates, by grade, then by from-date (a rate with none first). */
export function useRates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: ratesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Rate[]>(ratesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The percent ("12.50") of a grade's rate in force on a date (YYYY-MM-DD): the one whose from-date is the
 *  latest on or before it, a rate with no from-date applying from the beginning of time — or undefined when
 *  no rate applies then, or when that one's to-date is before the date. */
export function rateOn(rates: Rate[] | undefined, grade: string, date: string | undefined): string | undefined {
  if (date === undefined) return undefined;
  let found: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') <= (r.from ?? '')) found = r;
  }
  if (found?.to != null && found.to < date) return undefined;
  return found?.pct;
}

/** Whether a claim's live figures round half up: the date it rounds by (a correction's is the date of the claim it
 *  corrects) is before the sheet's policy date (its today when blank); else they round half to even. */
export function roundsHalfUp(c: Claim, settings: Settings | undefined, today: string | undefined): boolean {
  const policy = settings?.policyDate || today;
  return policy !== undefined && (c.roundsOn ?? c.date) < policy;
}

/** A claim's figure: a settled claim's frozen due, else the figure its person's last statement bound it to (not while PENDING),
 *  else its live due at the rate on its date of the grade (its person's grade on its date), at least the sheet's floor ("n/a" when it has none),
 *  rounded by the sheet's policy; a correction's live due has no floor. */
export function figureOf(c: Claim, rates: Rate[] | undefined, grade: string, floor: string | null | undefined,
  halfUp = false): string {
  if (c.due !== null) return c.due;
  if (isBound(c)) return c.statedDue ?? 'n/a';
  const due = dueOf(c.amount, rateOn(rates, grade, c.date), halfUp);
  return c.corrects !== null ? due : atLeast(due, floor);
}

/** A claim's due: a settled claim's frozen due, else its figure (at least the floor in force on its date) plus its
 *  late charge — the sheet's late percent in force on its date of the figure for every full 30 days it is overdue on the sheet's today, rounded like a due ("n/a" when it
 *  has no figure). A statement binds only the figure: the charge keeps following the sheet's today. A correction never
 *  takes a late charge. */
export function chargedOf(c: Claim, rates: Rate[] | undefined, grade: string, settings: Settings | undefined,
  today: string | undefined): string {
  const halfUp = roundsHalfUp(c, settings, today);
  const figure = figureOf(c, rates, grade, settings && settingOn(settings, 'floor', c.date, '1.00'), halfUp);
  if (c.due !== null || c.corrects !== null || figure === 'n/a') return figure;
  if (today === undefined || !isOverdue(c, today)) return figure;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${dueByOf(c)}T00:00:00Z`)) / 86_400_000;
  const periods = Math.floor(days / 30);
  const charge = dueOf(formatCents(hundredths(figure) * BigInt(periods)), settingOn(settings, 'late-pct', c.date, '0.00'), halfUp);
  return formatCents(hundredths(figure) + hundredths(charge));
}

/** A due raised to the floor when below it ("n/a" stays n/a; no floor leaves it as it is). */
export function atLeast(due: string, floor: string | null | undefined): string {
  if (due === 'n/a' || floor == null || floor === '') return due;
  return hundredths(due) < hundredths(floor) ? formatCents(hundredths(floor)) : due;
}

/** A two-decimal string ("12.50") in hundredths, exactly. */
const hundredths = (s: string) => BigInt(Math.round(Number(s) * 100));

/**
 * The due on an amount: amount times the percent, divided by 100, rounded half to even (or half up, away from
 * zero) to two decimals — "n/a" when there is no percent. Computed in whole hundredths, so it is exact.
 */
export function dueOf(amount: string, pct: string | undefined, halfUp = false): string {
  if (pct === undefined) return 'n/a';
  // hundredths of the amount times hundredths of the percent = the due in millionths of a percent;
  // dividing by 10000 gives cents.
  const product = hundredths(amount) * hundredths(pct);
  const negative = product < 0n;
  const magnitude = negative ? -product : product;
  const rest = magnitude % 10000n;
  let cents = magnitude / 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return formatCents(negative ? -cents : cents);
}

/** Whole cents as a two-decimal string. */
export function formatCents(cents: bigint): string {
  const negative = cents < 0n;
  const m = negative ? -cents : cents;
  return `${negative ? '-' : ''}${m / 100n}.${(m % 100n).toString().padStart(2, '0')}`;
}

/** Adds up two-decimal amounts, skipping "n/a". */
export function sumDue(dues: string[]): string {
  return formatCents(dues.filter((d) => d !== 'n/a').reduce((n, d) => n + hundredths(d), 0n));
}

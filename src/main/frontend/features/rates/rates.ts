import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { isBound, isOverdue, lateFromOf, type Claim } from '../claims/claims';
import { settingOn, type Settings } from '../roles/roles';
import { useCurrentSheet } from '../session/session';

/** A grade's rate as the store holds it: the percent with two decimals, from a date on (YYYY-MM-DD; null
 *  when it applies from the beginning of time), through a to-date (null when it has no end), and the sheet it is set
 *  on — the sheet's own, or one up its chain of parents; the newest of its versions, with how many it has; and each
 *  time it was set, in order (empty when not known: it applies to every claim as it is). */
export type Rate = {
  grade: string; pct: string; from: string | null; to: string | null; sheet: string; version: number;
  layers?: RateLayer[];
};

/** One time a rate was set: its percent and to-date then, and, when it was forward-only (a from-date on or after the
 *  sheet's today then), the highest claim id there was (null: it applies to every claim). */
export type RateLayer = { pct: string; to: string | null; after: number | null };

/** A rate as it applies to a claim (by its id): the last time it was set that applies to it — not forward-only, or
 *  forward-only before the claim was added; undefined when none does. As it is when its settings are not known. */
export function rateForClaim(r: Rate, claimId: number): Rate | undefined {
  const layers = r.layers ?? [];
  if (layers.length === 0) return r;
  let applies: RateLayer | undefined;
  for (const l of layers) if (l.after === null || claimId > l.after) applies = l;
  if (applies === layers[layers.length - 1]) return r;
  return applies && { ...r, pct: applies.pct, to: applies.to };
}

/** The rates that apply to a claim (by its id), as {@link rateForClaim}. */
export function ratesForClaim(rates: Rate[] | undefined, claimId: number): Rate[] {
  return (rates ?? []).flatMap((r) => rateForClaim(r, claimId) ?? []);
}

export const ratesQueryKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

export const ratesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rates`;

/** Where a candidate rate is previewed: what it would change, saving nothing. */
export const ratesPreviewPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/rate-preview`;

/** The sheet's rates, by grade, then by from-date (a rate with none first); then those of its parent, its parent's
 *  parent, and one more up, each in the same order. */
export function useRates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: ratesQueryKey(sheet ?? ''),
    queryFn: () => getJson<Rate[]>(ratesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The percent ("12.50") of a grade's rate in force on a date (YYYY-MM-DD): the one whose from-date is the
 *  latest on or before it, a rate with no from-date applying from the beginning of time — or, when no rate applies
 *  then on the sheet, or that one's to-date is before the date, the next sheet's up its chain of parents (the rates
 *  come sheet by sheet, the sheet's own first); undefined when none has one. */
export function rateOn(rates: Rate[] | undefined, grade: string, date: string | undefined): string | undefined {
  if (date === undefined) return undefined;
  for (const sheet of [...new Set((rates ?? []).map((r) => r.sheet))]) {
    let found: Rate | undefined;
    for (const r of rates ?? []) {
      if (r.sheet !== sheet || r.grade !== grade || (r.from !== null && r.from > date)) continue;
      if (!found || (found.from ?? '') <= (r.from ?? '')) found = r;
    }
    if (found && (found.to == null || found.to >= date)) return found.pct;
  }
  return undefined;
}

/** Whether a claim's live figures round half up: by the sheet's rounding mode in force on the date it rounds by (a
 *  correction's is the date of the claim it corrects) when one is; else when that date is before the sheet's policy
 *  date (its today when blank); else they round half to even. */
export function roundsHalfUp(c: Claim, settings: Settings | undefined, today: string | undefined): boolean {
  const mode = settings && settingOn(settings, 'rounding', c.roundsOn ?? c.date, '');
  if (mode === 'half-up' || mode === 'half-even') return mode === 'half-up';
  const policy = settings?.policyDate || today;
  return policy !== undefined && (c.roundsOn ?? c.date) < policy;
}

/** A claim's figure: a settled claim's frozen due, else the figure its person's last statement bound it to,
 *  else its rate figure at the rate on its date (of those that apply to it: a forward-only rate saved after it was added does not) of the grade (its person's grade on its date) ("n/a" when it has none),
 *  rounded by the sheet's policy; no floor (that comes last, in {@link chargedOf}). */
export function figureOf(c: Claim, rates: Rate[] | undefined, grade: string, halfUp = false): string {
  if (c.due !== null) return c.due;
  if (isBound(c)) return c.statedDue ?? 'n/a';
  return dueOf(c.amount, rateOn(ratesForClaim(rates, c.id), grade, c.date), halfUp);
}

/** A claim's due: a settled claim's frozen due, else the greater of the sheet's floor in force on its date and its figure
 *  plus its late charge — for each full 30 days it is overdue on the sheet's today, the sheet's late percent in force on that
 *  boundary date (its due-by plus its grade's grace, plus 30, 60, 90 ... days) of the figure plus the charges of the boundaries
 *  before it, rounded like a due, added up; a boundary at 0.00 adds
 *  nothing ("n/a" when it has no figure). A statement binds the due as shown, the charge and the floor included: while
 *  bound it no longer follows the sheet's today. A correction takes neither floor nor late charge; a claim whose late charge is waived takes no late charge. */
export function chargedOf(c: Claim, rates: Rate[] | undefined, grade: string, settings: Settings | undefined,
  today: string | undefined): string {
  if (c.due === null && isBound(c)) return c.statedDue ?? 'n/a';
  const halfUp = roundsHalfUp(c, settings, today);
  const figure = figureOf(c, rates, grade, halfUp);
  if (c.due !== null || c.corrects !== null || figure === 'n/a') return figure;
  const floor = settings && settingOn(settings, 'floor', c.date, '1.00');
  if (today === undefined || c.waived || !isOverdue(c, today, grade, settings)) return atLeast(figure, floor);
  const dueBy = Date.parse(`${lateFromOf(c, grade, settings)}T00:00:00Z`);
  const periods = Math.floor((Date.parse(`${today}T00:00:00Z`) - dueBy) / 86_400_000 / 30);
  let charge = 0n;
  for (let k = 1; k <= periods; k++) {
    const boundary = new Date(dueBy + k * 30 * 86_400_000).toISOString().slice(0, 10);
    const pct = settingOn(settings, 'late-pct', boundary, '0.00');
    if (hundredths(pct) !== 0n) charge += hundredths(dueOf(formatCents(hundredths(figure) + charge), pct, halfUp));
  }
  return atLeast(formatCents(hundredths(figure) + charge), floor);
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

/** One two-decimal amount minus another, as a two-decimal string (a minus sign when below zero). */
export function diffDue(a: string, b: string): string {
  return formatCents(hundredths(a) - hundredths(b));
}

/** The sheet's total of people's figures, skipping "n/a": with three or more figures, the single highest and the
 *  single lowest are left out. */
export function trimmedSum(dues: string[]): string {
  const figures = dues.filter((d) => d !== 'n/a').map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = figures.length >= 3 ? figures.slice(1, -1) : figures;
  return formatCents(kept.reduce((n, d) => n + d, 0n));
}

import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, dueBy, live, overdue, type Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';

/** A grade's rate on the sheet: a percent with two decimals, in force from its from-date (yyyy-mm-dd, or
 *  null: from the beginning of time). A grade may have several, one per from-date; none when it has no rate. */
export type Rate = { grade: string; rate: string; from: string | null };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, every grade's every from-date. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** A grade's rate in force on a date (yyyy-mm-dd): the one whose from-date is the latest on or before
 *  it, a rate with no from-date applying from the beginning of time; undefined when none applies. */
export function rateOn(rates: Rate[], grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.rate;
}

/** A grade's rate in force on a date, or undefined when none applies (or the rates are still loading). */
export function useRateFor(sheetId: string, grade: string, date: string): string | undefined {
  return rateOn(useRates(sheetId).data ?? [], grade, date);
}

/** A two-decimal amount (may be negative) as whole cents, exactly. */
function toCents(amount: string): bigint {
  const trimmed = amount.trim();
  const negative = trimmed.startsWith('-');
  const [whole, frac = ''] = (negative ? trimmed.slice(1) : trimmed).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

function fromCents(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
}

/** Due on an amount: amount times rate, divided by 100, rounded to two decimals half to even, or half up (away
 *  from zero) when `halfUp` (exact, in integers); 'n/a' when there is no rate. */
export function dueOn(amount: string, rate: string | undefined, halfUp = false): string {
  if (rate === undefined) return 'n/a';
  const scaled = toCents(amount) * toCents(rate); // in 1/10000 of a cent
  const sign = scaled < 0n ? -1n : 1n;
  const abs = scaled * sign;
  let cents = abs / 10000n;
  const rest = abs % 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return fromCents(cents * sign);
}

/** Two-decimal amounts added up exactly, skipping 'n/a'. */
export function sumDue(dues: string[]): string {
  return fromCents(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + toCents(d), 0n));
}

/** A person's due-total: the figures of their claims that count (each as shown on its row) added up;
 *  'n/a' when their grade today has no rate today (rateToday undefined), unless a claim that counts has a
 *  figure (settled, stated, or live at the grade in force on its date), then the figures there are added up. */
export function personDue(claims: Claim[], rateOnDate: (date: string) => string | undefined,
    rateToday: string | undefined, floor: string, late: Late): string {
  const counted = claims.filter(counts);
  const dues = counted.map((c) => claimDue(c, rateOnDate(c.date), floor, late));
  if (rateToday === undefined && dues.every((d) => d === 'n/a')) return 'n/a';
  return sumDue(dues);
}

/** A person's due-total as shown on their row: their live claims among these (see live) through personDue, at
 *  the grade in force on each claim's date and on the sheet's today. */
export function personDueTotal(person: Person, claims: Claim[], rates: Rate[], today: string, floor: string,
    late: Late): string {
  return personDue(live(claims.filter((c) => c.personId === person.id), [person]),
    (d) => rateOn(rates, gradeOn(person, d), d), rateOn(rates, gradeOn(person, today), today), floor, late);
}

/** What the late charge follows: the sheet's terms in days, its late percent (two decimals) and its today; and
 *  how a claim's live figures round: the policy date (the policy-date setting, else the sheet's today; '' while
 *  loading) and the dates of the sheet's numbered claims (a correction rounds by the claim it corrects). */
export type Late = { terms: number; pct: string; today: string; policy: string; numbered: Map<string, string> };

/** Whether a claim's live figures round half up: it (a correction: the claim it corrects) is dated before the
 *  policy date; else they round half to even. */
export function roundsUp(claim: Claim, late: Late): boolean {
  const date = claim.corrects !== null ? late.numbered.get(claim.corrects) ?? claim.date : claim.date;
  return late.policy !== '' && date < late.policy;
}

/** A claim's due: the one fixed when it was settled (its late charge frozen in), else its figure — the one its
 *  person's last statement bound it to, else live on its amount at the rate, raised to the sheet's floor when
 *  below it — with the late charge on that figure added on top. A correction (a negative amount) has no floor
 *  and no late charge. */
export function claimDue(claim: Claim, rate: string | undefined, floor: string, late: Late): string {
  if (claim.settledDue !== null) return claim.settledDue;
  const correction = toCents(claim.amount) < 0n;
  const live = dueOn(claim.amount, rate, roundsUp(claim, late));
  const figure = claim.statedDue ?? (correction ? live : floored(live, floor));
  if (correction) return figure;
  return figure === 'n/a' ? figure : fromCents(toCents(figure) + toCents(lateCharge(claim, figure, late)));
}

const DAY_MS = 24 * 60 * 60 * 1000;

/** The late charge on a claim whose figure is `figure`: the late percent of it for every full 30 days the
 *  claim is overdue (past its due-by, on the sheet's today), rounded like the claim's due; 0.00 when not overdue, or a
 *  correction (a negative amount). */
export function lateCharge(claim: Claim, figure: string, late: Late): string {
  if (!overdue(claim, late.terms, late.today) || toCents(claim.amount) < 0n) return '0.00';
  const days = Math.round((Date.parse(`${late.today}T00:00:00Z`) - Date.parse(`${dueBy(claim, late.terms)}T00:00:00Z`)) / DAY_MS);
  const periods = BigInt(Math.floor(days / 30));
  return dueOn(figure, fromCents(toCents(late.pct) * periods), roundsUp(claim, late));
}

/** A live due raised to the floor when below it; 'n/a' stays 'n/a'. */
export function floored(due: string, floor: string): string {
  return due === 'n/a' || toCents(due) >= toCents(floor) ? due : fromCents(toCents(floor));
}


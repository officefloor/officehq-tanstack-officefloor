import { type Claim, isOpen } from '../claims/claims';
import { type Floor, type Late, type Rate, claimDue, dueTotal } from '../rates/rates';
import { type Settings, graceOf, latePctOf, roundingOf } from '../settings/settings';
import { type Grades, type Person, gradeOn } from '../people/people';

const DAY = 24 * 60 * 60 * 1000;

/** The claim's due-by (yyyy-MM-dd): fixed when it was added, from the terms of its person's grade then. */
export const dueBy = (claim: Claim): string => claim.dueBy;

/** The date (yyyy-MM-dd) the claim's overdue counts from: its due-by plus the grace days in force on its date for
 *  its person's grade then (its due-by when none, or when the late charge is not known). */
export const graceBy = (claim: Claim, late?: Late): string => late?.graceBy?.(claim) ?? dueBy(claim);

/** Whether the claim is overdue: it is OPEN (or PENDING), not deferred, and its due-by plus any grace is before the sheet's today. */
export function overdue(claim: Claim, today: string | undefined, late?: Late): boolean {
  return isOpen(claim) && !claim.deferred && today !== undefined && graceBy(claim, late) < today;
}

/** The date `days` after the date (yyyy-MM-dd). */
const plusDays = (date: string, days: number): string =>
  new Date(Date.parse(`${date}T00:00:00Z`) + days * DAY).toISOString().slice(0, 10);

/** Whether the claim is review-late: it is REVIEW and its review day plus the sheet's review SLA (whole days; null or
 *  blank for none) is before the sheet's today. */
export function reviewLate(claim: Claim, today: string | undefined, sla: string | null | undefined): boolean {
  return claim.status === 'REVIEW' && claim.reviewOn !== null && today !== undefined && sla != null && sla !== ''
    && plusDays(claim.reviewOn, Number(sla)) < today;
}

/** How many full 30 days the claim is overdue on the sheet's today, counted from its due-by plus any grace (0 when it is not overdue). */
export function latePeriods(claim: Claim, today: string | undefined, late?: Late): number {
  if (today === undefined || !overdue(claim, today, late)) return 0;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${graceBy(claim, late)}T00:00:00Z`)) / DAY;
  return Math.floor(days / 30);
}

/** The 30-day boundaries (yyyy-MM-dd) the claim is overdue past on the sheet's today: its due-by plus any grace, plus 30, 60, 90 ... days. */
export function lateBoundaries(claim: Claim, today: string | undefined, late?: Late): string[] {
  const start = Date.parse(`${graceBy(claim, late)}T00:00:00Z`);
  return Array.from({ length: latePeriods(claim, today, late) }, (_, i) =>
    new Date(start + (i + 1) * 30 * DAY).toISOString().slice(0, 10));
}

/** What the sheet's late charge follows: its late percent in force on each boundary date, each claim's due-by plus
 *  the grace days in force on its date for its person's grade then (of these people), and its today; and how it rounds:
 *  a claim rounding by a date with a rounding mode in force rounds by it, else one rounding by a date before the policy
 *  date (else the sheet's today) rounds half up. */
export function lateOf(settings: Settings | undefined, today: string | undefined, people: Person[] | undefined): Late {
  const policy = settings?.policyDate ?? today;
  const pcts = latePctOf(settings);
  const roundings = roundingOf(settings);
  const graces = graceOf(settings);
  const late: Late = {
    pct: pcts,
    boundaries: (c) => lateBoundaries(c, today, late),
    halfUp: (c) => {
      const mode = roundings?.(c.roundsBy ?? c.date);
      if (mode === 'half-up' || mode === 'half-even') return mode === 'half-up';
      return policy !== undefined && (c.roundsBy ?? c.date) < policy;
    },
    graceBy: (c) => {
      const person = people?.find((p) => p.id === c.personId);
      const grace = person && graces?.(gradeOn(person.grades, c.date), c.date);
      return grace == null ? dueBy(c) : plusDays(dueBy(c), grace);
    },
  };
  return late;
}

/** A person's overdue claims among these (their claims that count): how many, and the sum of their
 *  dues (late charge included) at the person's grade in force on each claim's date, skipping n/a. */
export function overdueFigures(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  floor: Floor,
  today: string | undefined,
  lateCharge?: Late,
): { count: number; total: string } {
  const late = claims.filter((c) => overdue(c, today, lateCharge));
  return { count: late.length, total: dueTotal(late.map((c) => claimDue(c, rates, grades, floor, lateCharge))) };
}

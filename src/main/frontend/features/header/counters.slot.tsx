import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentUser, useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { overdueOf } from '../claims/overdue.slot';
import { personDue } from '../rates/due.slot';
import { sumDue, trimmedSum, useRates, type Rate } from '../rates/rates';
import { useSettings, type Settings } from '../roles/roles';
import type { Person } from '../people/people';
import type { Claim } from '../claims/claims';
import { STATUSES } from '../summary/summary.slot';
import { useCrews } from '../crews/crews';
import { crewDue } from '../crews/crews.slot';
import { useWatched } from '../people/watch.slot';
import { usePools, type Pool } from '../pools/pools';

/** A sheet's due-total and overdue-total, as the rollup figures them, from what the store holds for it. */
export function sheetTotals(people: Person[], claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined, pools: Pool[] | undefined): { due: string; overdue: string } {
  return {
    due: trimmedSum(people.map((p) => personDue(p, claims, rates, today, settings, people, pools))),
    overdue: trimmedSum(people.map((p) => overdueOf(p, claims, rates, today, settings).total)),
  };
}

/** The person on the sheet going by the user's name, as the server finds one: by name first, then by alias,
 *  ignoring case (undefined: none). */
export function personNamed(people: Person[], user: string | undefined): Person | undefined {
  if (!user) return undefined;
  const key = user.toLowerCase();
  return people.find((p) => p.name.toLowerCase() === key)
    ?? people.find((p) => p.aliases.some((a) => a.toLowerCase() === key));
}

// The header counters, shown at all times: how many people the sheet has (retired ones included), how many of its
// claims have each status (as the summary counts them), and the sheet's due-total and overdue-total (as the rollup
// figures them); and the signed-in user's own due-total (as their row shows it, "n/a" included) and overdue-total, when a person on the sheet goes by their
// name (blank when none does), with their crew total: the due-totals of the crews that person leads, added up (blank
// when they lead none); the due-totals of the people the signed-in user watches, added up (0.00 when watching no one);
// and how many REVIEW claims are assigned to the signed-in user.
function HeaderCounters() {
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const crews = useCrews();
  const settings = useSettings();
  const pools = usePools();
  const watched = useWatched();
  const today = useSheetToday();
  const allPeople = people.data ?? [];
  const allClaims = claims.data ?? [];
  const { due, overdue } = sheetTotals(allPeople, allClaims, rates.data, today, settings.data, pools.data);
  const [user] = useCurrentUser();
  const me = personNamed(allPeople, user);
  const mine = me
    ? { due: personDue(me, allClaims, rates.data, today, settings.data, allPeople, pools.data),
        overdue: sheetTotals([me], allClaims, rates.data, today, settings.data, pools.data).overdue }
    : { due: '', overdue: '' };
  const led = me ? (crews.data ?? []).filter((c) => c.lead?.id === me.id) : [];
  const myCrewDue = led.length > 0
    ? sumDue(led.map((c) => crewDue(c, allPeople, allClaims, rates.data, today, settings.data, pools.data)))
    : '';
  const watchedIds = watched.data ?? [];
  const watchedDue = sumDue(allPeople.filter((p) => watchedIds.includes(p.id))
    .map((p) => personDue(p, allClaims, rates.data, today, settings.data, allPeople, pools.data)));
  const myReviews = allClaims.filter((c) => c.status === 'REVIEW' && !!user && c.reviewAssigned === user).length;
  return (
    <section data-testid="header-counters">
      People <strong data-testid="header-people-count">{allPeople.length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}{s} <strong data-testid={`header-claims-${s}-count`}>{allClaims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}
      {' · '}Due <strong data-testid="header-due-total">{due}</strong>
      {' · '}Overdue <strong data-testid="header-overdue-total">{overdue}</strong>
      {' · '}My due <strong data-testid="my-due-total">{mine.due}</strong>
      {' · '}My overdue <strong data-testid="my-overdue-total">{mine.overdue}</strong>
      {' · '}My crew due <strong data-testid="my-crew-due-total">{myCrewDue}</strong>
      {' · '}Watched due <strong data-testid="watched-due-total">{watchedDue}</strong>
      {' · '}My reviews <strong data-testid="my-reviews-count">{myReviews}</strong>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 1, Component: HeaderCounters });

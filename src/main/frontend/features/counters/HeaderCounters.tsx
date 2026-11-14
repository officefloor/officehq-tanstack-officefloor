import { useAliases } from '../aliases/aliases';
import type { Alias } from '../aliases/aliases';
import { useClaims } from '../claims/claims';
import { crewDue, useCrews, useDueByPerson } from '../crews/crews';
import type { Claim } from '../claims/claims';
import { personOverdue } from '../overdue/overdue';
import { liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personDue, total, trimmedTotal, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { useDueRules } from '../settings/settings';
import type { DueRules } from '../settings/settings';
import { useCurrentUser } from '../session/session';

/** The statuses counted, as in the summary; one no claim reaches yet still shows, as 0. */
export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'] as const;

/** A sheet's due-total and overdue-total as its header shows them. */
export function sheetTotals(people: Person[], claims: Claim[], rates: Rate[], rules: DueRules): { due: string; overdue: string } {
  const mine = (id: number) => claims.filter((c) => c.personId === id);
  const dues = people.map((p) =>
    personDue(mine(p.id), rates, p.grades, liveHolds(p, rules.today), rules, { personId: p.id, claims, people }),
  );
  const overdue = people.map((p) => personOverdue(mine(p.id), rates, p.grades, liveHolds(p, rules.today), rules).total);
  return { due: trimmedTotal(dues), overdue: trimmedTotal(overdue) };
}

/** A name as names are compared: trimmed, ignoring case. */
export const nameKey = (name: string) => name.trim().toLowerCase();

/** The person on the sheet with the user's name, by name or alias, if any. */
export function personNamed(people: Person[], aliases: Alias[], user: string): Person | undefined {
  const key = nameKey(user);
  if (!key) return undefined;
  return (
    people.find((p) => nameKey(p.name) === key) ??
    people.find((p) => aliases.some((a) => a.personId === p.id && nameKey(a.alias) === key))
  );
}

// The sheet's counters, always in the header: everyone on it (retired people included), its claims per
// status, its due-total and overdue-total, the signed-in user's own (blank when no one on it has their name), and the due-totals of the crews that person
// leads added up (blank when they lead none), the REVIEW claims assigned to the user — each worked out exactly as the figure it repeats.
export function HeaderCounters() {
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const aliases = useAliases().data ?? [];
  const crews = useCrews().data ?? [];
  const dueOf = useDueByPerson();
  const [user] = useCurrentUser();

  const me = personNamed(people, aliases, user ?? '');
  const mine = me ? claims.filter((c) => c.personId === me.id) : [];
  const myDue = me ? personDue(mine, rates, me.grades, liveHolds(me, rules.today), rules, { personId: me.id, claims, people }) : '';
  const myOverdue = me ? personOverdue(mine, rates, me.grades, liveHolds(me, rules.today), rules).total : '';
  const led = me ? crews.filter((c) => c.lead?.id === me.id) : [];
  const myCrewDue = led.length ? total(led.map((c) => crewDue(c, dueOf))) : '';
  const myReviews = claims.filter((c) => c.status === 'REVIEW' && c.reviewAssigned === (user ?? '').trim()).length;
  const totals = sheetTotals(people, claims, rates, rules);
  return (
    <p data-testid="header-counters">
      People: <strong data-testid="header-people-count">{people.length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s}: <strong data-testid={`header-claims-${s}-count`}>{claims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}
      {' · '}Due: <strong data-testid="header-due-total">{totals.due}</strong>
      {' · '}Overdue: <strong data-testid="header-overdue-total">{totals.overdue}</strong>
      {' · '}Mine: <strong data-testid="my-due-total">{myDue}</strong>
      {' / '}<strong data-testid="my-overdue-total">{myOverdue}</strong>
      {' · '}My crews: <strong data-testid="my-crew-due-total">{myCrewDue}</strong>
      {' · '}My reviews: <strong data-testid="my-reviews-count">{myReviews}</strong>
    </p>
  );
}

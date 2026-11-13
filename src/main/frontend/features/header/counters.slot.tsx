import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { STATUSES, statusCount, useClaims } from '../claims/claims';
import { type Person, usePeople } from '../people/people';
import { usePersonDue, useSheetDueTotal } from '../rates/due.slot';
import { dueTotal } from '../rates/rates';
import { crewDue, useCrews } from '../crews/crews';
import { useOverdue, useSheetOverdue } from '../overdue/overdue.slot';
import { useFigure } from '../locale/locale';

/** Names are compared ignoring case and the spaces around them. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The person on the sheet with the user's name, by name or alias (undefined when none). */
function personNamed(people: Person[] | undefined, user: string): Person | undefined {
  const key = nameKey(user);
  if (key === '') return undefined;
  return (people ?? []).find((p) => nameKey(p.name) === key || p.aliases.some((a) => nameKey(a) === key));
}

// The sheet's counters, always in the header: its people (retired people included), its claims per
// status (the summary's counts) and its due- and overdue-totals (the rollup's), from the same figures;
// then the signed-in user's own due- and overdue-totals (their person's, blank when no person has their name),
// and their crew total: the due-totals of the crews their person leads, added up (blank when they lead none),
// and how many REVIEW claims are assigned to them.
function HeaderCounters() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const due = useSheetDueTotal();
  const overdue = useSheetOverdue();
  const [user] = useCurrentUser();
  const me = personNamed(people.data, user);
  const myDue = usePersonDue();
  const myOverdue = useOverdue();
  const crews = useCrews(sheet);
  const led = me ? (crews.data ?? []).filter((c) => c.leadId === me.id) : [];
  const myReviews = (claims.data ?? []).filter(
    (c) => c.status === 'REVIEW' && c.reviewUser != null && nameKey(c.reviewUser) === nameKey(user),
  ).length;
  const myCrewDue = led.length > 0 ? dueTotal(led.map((c) => crewDue(c, people.data ?? [], myDue))) : '';

  return (
    <p data-testid="header-counters">
      People <strong data-testid="header-people-count">{(people.data ?? []).length}</strong>{' '}
      {STATUSES.map((s) => (
        <span key={s}>
          {s} <strong data-testid={`header-claims-${s}-count`}>{statusCount(claims.data, s)}</strong>{' '}
        </span>
      ))}
      Due total <strong data-testid="header-due-total">{fig(due)}</strong>{' '}
      Overdue total <strong data-testid="header-overdue-total">{fig(overdue.total)}</strong>{' '}
      My due total <strong data-testid="my-due-total">{fig(me ? myDue(me) : '')}</strong>{' '}
      My overdue total <strong data-testid="my-overdue-total">{fig(me ? myOverdue(me).total : '')}</strong>{' '}
      My crew total <strong data-testid="my-crew-due-total">{fig(myCrewDue)}</strong>{' '}
      My reviews <strong data-testid="my-reviews-count">{myReviews}</strong>
    </p>
  );
}

export const contribution = AppHeader.fill({ order: 30, Component: HeaderCounters });

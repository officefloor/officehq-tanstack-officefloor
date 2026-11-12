import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { STATUSES, statusCount, useClaims } from '../claims/claims';
import { type Person, usePeople } from '../people/people';
import { usePersonDue, useSheetDueTotal } from '../rates/due.slot';
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
// then the signed-in user's own due- and overdue-totals (their person's, blank when no person has their name).
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
      My overdue total <strong data-testid="my-overdue-total">{fig(me ? myOverdue(me).total : '')}</strong>
    </p>
  );
}

export const contribution = AppHeader.fill({ order: 30, Component: HeaderCounters });

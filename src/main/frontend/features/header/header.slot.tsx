import { SheetBody } from '../../slots/defs/sheet';
import { crewDueTotal, useCrews } from '../crews/crews';
import { STATUSES, useClaims } from '../claims/claims';
import { usePeople, type Person } from '../people/people';
import { overdueDues, personDueTotal, sheetDueTotal, sheetOverdueTotal, sumDue, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { useWatches } from '../watch/watch';

/** The sheet's header counters, shown above everything else: its people (retired ones included), its claims
 *  per status (as the summary counts them) and its due-total and overdue-total (as the rollup shows them); and the
 *  signed-in user's own figures: the due-total and overdue-total of the person whose name or alias is the user's
 *  name (as names are compared), shown on their row; nothing when there is no such person; and the user's crew
 *  total: the due-totals of the crews that person leads (as shown on the crew rows), added up; nothing when they
 *  lead no crew; the count of REVIEW claims assigned to the user to review; and the due-totals of the people the
 *  user watches (as shown on their rows), added up; 0.00 when they watch no one. */
function Header({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const crews = useCrews(sheetId).data ?? [];
  const me = personNamed(people, user);
  const due = (p: Person) => personDueTotal(p, claims, rates, today, floor, late, cap, people);
  const watched = new Set(useWatches(sheetId, user).data ?? []);
  const led = me ? crews.filter((c) => c.leadId === me.id) : [];

  return (
    <header data-testid="header">
      <span>
        People <strong data-testid="header-people-count">{people.length}</strong>
      </span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}
          {s} <strong data-testid={`header-claims-${s}-count`}>{claims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}{' '}
      <span>
        Due total <strong data-testid="header-due-total">{sheetDueTotal(people, claims, rates, today, floor, late, cap)}</strong>
      </span>{' '}
      <span>
        Overdue total <strong data-testid="header-overdue-total">{sheetOverdueTotal(people, claims, rates, late, floor)}</strong>
      </span>{' '}
      <span>
        My due total <strong data-testid="my-due-total">
          {me ? due(me) : ''}
        </strong>
      </span>{' '}
      <span>
        My overdue total <strong data-testid="my-overdue-total">
          {me ? sumDue(overdueDues(claims.filter((c) => c.personId === me.id), [me], rates, late, floor)) : ''}
        </strong>
      </span>{' '}
      <span>
        My crew due total <strong data-testid="my-crew-due-total">
          {led.length > 0 ? sumDue(led.map((c) => crewDueTotal(c, people, due))) : ''}
        </strong>
      </span>{' '}
      <span>
        My reviews <strong data-testid="my-reviews-count">
          {claims.filter((c) => c.status === 'REVIEW' && c.reviewer === user).length}
        </strong>
      </span>{' '}
      <span>
        Watched due total <strong data-testid="watched-due-total">
          {sumDue(people.filter((p) => watched.has(p.id)).map(due))}
        </strong>
      </span>
    </header>
  );
}

/** A name as names are compared: without regard to case, ignoring surrounding spaces. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The person with this name or alias (as names are compared), if any. */
function personNamed(people: Person[], name: string): Person | undefined {
  const key = nameKey(name);
  return people.find((p) => nameKey(p.name) === key || p.aliases.some((a) => nameKey(a) === key));
}

export const contribution = SheetBody.fill({ order: -1, Component: Header });

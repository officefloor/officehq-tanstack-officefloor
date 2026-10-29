import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { crewDue } from '../crews/crews.slot';
import { crewsQuery } from '../crews/crewsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { usePeopleDueTotals, usePersonDueTotal, useSheetDueTotal } from '../rates/due.slot';
import { usePersonOverdueTotal, useSheetOverdue } from '../rates/overdue.slot';
import { useUser } from '../session/useUser';
import { sumDue } from '../rates/ratesQuery';
import { STATUSES, statusCount } from '../summary/summary.slot';

/**
 * Counters shown at the top of the sheet at all times. Each repeats a figure shown elsewhere and is
 * worked out the same way: people on the sheet (retired included), claims per status as in the
 * summary, and the sheet's due- and overdue-totals. Then the signed-in user's own figures: the due- and
 * overdue-total of the person whose name or alias is the user's name (as names are compared: trimmed, ignoring
 * case); blank when there is no such person. And the user's crew total: the due-totals of the crews that person
 * leads, added up; blank when they lead none. And how many REVIEW claims are assigned to the user (as users are
 * compared: trimmed).
 */
const nameKey = (name: string) => name.trim().toLowerCase();

function Header({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const dueTotal = useSheetDueTotal(sheetId);
  const { total: overdueTotal } = useSheetOverdue(sheetId);
  const [user] = useUser();
  const me = nameKey(user);
  const mine = me ? people.find((p) => [p.name, ...p.aliases].some((n) => nameKey(n) === me)) : undefined;
  const myDue = usePersonDueTotal(sheetId, mine?.id);
  const myOverdue = usePersonOverdueTotal(sheetId, mine?.id);
  const crews = useQuery(crewsQuery(sheetId)).data ?? [];
  const dues = usePeopleDueTotals(sheetId);
  const led = mine ? crews.filter((c) => c.leadId === mine.id) : [];
  const myReviews = claims.filter((c) => c.status === 'REVIEW' && (c.reviewUser ?? '').trim() === user.trim()).length;
  const myCrewDue = led.length > 0 ? sumDue(led.map((c) => crewDue(c, dues))) : '';

  return (
    <header data-testid="header">
      People <strong data-testid="header-people-count">{people.length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}· {s} <strong data-testid={`header-claims-${s}-count`}>{statusCount(claims, s)}</strong>
        </span>
      ))}
      {' '}· Due <strong data-testid="header-due-total">{dueTotal}</strong>
      {' '}· Overdue <strong data-testid="header-overdue-total">{overdueTotal}</strong>
      {' '}· Mine due <strong data-testid="my-due-total">{myDue ?? ''}</strong>
      {' '}· Mine overdue <strong data-testid="my-overdue-total">{myOverdue ?? ''}</strong>
      {' '}· Crew due <strong data-testid="my-crew-due-total">{myCrewDue}</strong>
      {' '}· My reviews <strong data-testid="my-reviews-count">{myReviews}</strong>
    </header>
  );
}

export const contribution = SheetPanel.fill({ order: -10, Component: Header });

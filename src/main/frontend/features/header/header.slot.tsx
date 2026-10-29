import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { usePersonDueTotal, useSheetDueTotal } from '../rates/due.slot';
import { usePersonOverdueTotal, useSheetOverdue } from '../rates/overdue.slot';
import { useUser } from '../session/useUser';
import { STATUSES, statusCount } from '../summary/summary.slot';

/**
 * Counters shown at the top of the sheet at all times. Each repeats a figure shown elsewhere and is
 * worked out the same way: people on the sheet (retired included), claims per status as in the
 * summary, and the sheet's due- and overdue-totals. Then the signed-in user's own figures: the due- and
 * overdue-total of the person whose name or alias is the user's name (as names are compared: trimmed, ignoring
 * case); blank when there is no such person.
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
    </header>
  );
}

export const contribution = SheetPanel.fill({ order: -10, Component: Header });

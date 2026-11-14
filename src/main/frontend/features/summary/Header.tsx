import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { useDueTotal } from '../rates/DueTotal';
import { useOverdue } from '../rates/Overdue';
import { STATUSES, statusCount } from './Summary';

/** The sheet's header counters, shown at all times: everyone on the sheet (retired included), the
 *  claims per status (as the summary counts them), and the sheet's due- and overdue-totals. */
export function Header({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const dueTotal = useDueTotal(sheetId, user);
  const late = useOverdue(sheetId, user);
  return (
    <header>
      People <span data-testid="header-people-count">{(people.data ?? []).length}</span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}{s} <span data-testid={`header-claims-${s}-count`}>{statusCount(claims.data, s)}</span>
        </span>
      ))}
      {' · '}Due <span data-testid="header-due-total">{dueTotal}</span>
      {' · '}Overdue <span data-testid="header-overdue-total">{late.total}</span>
    </header>
  );
}

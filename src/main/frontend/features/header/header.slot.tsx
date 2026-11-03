import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { useOverdue } from '../overdue/useOverdue';
import { useAliases, usePeople } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { trimmedSum } from '../rates/useRates';
import { useSignedInUser } from '../session/useSession';
import { countWithStatus, STATUSES } from '../summary/statusCounts';

/** The sheet's header counters, shown at all times: how many people are on the sheet (retired people
 *  included), how many claims have each status (as the summary counts them), and the sheet's
 *  due-total and overdue-total (as the totals show them, trimmed); and the signed-in user's own figures: the
 *  due-total and overdue-total of the person whose name or an alias is the user's name (ignoring case), blank
 *  when there is no such person. */
function Header({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const dueTotals = useDueTotals(sheetId);
  const overdueById = useOverdue(sheetId);
  const overdue = [...overdueById.values()];
  const aliases = useAliases(sheetId);
  const [user] = useSignedInUser();
  const me = user.trim().toLowerCase();
  const mine = me === '' ? undefined : (people.data ?? []).find((p) => p.name.trim().toLowerCase() === me
    || (aliases.data ?? []).some((a) => a.personId === p.id && a.alias.trim().toLowerCase() === me));
  return (
    <header data-testid="header">
      People <strong data-testid="header-people-count">{(people.data ?? []).length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}
          · {s} <strong data-testid={`header-claims-${s}-count`}>{countWithStatus(claims.data, s)}</strong>
        </span>
      ))}{' '}
      · Due total <strong data-testid="header-due-total">{trimmedSum([...dueTotals.values()])}</strong> · Overdue
      total <strong data-testid="header-overdue-total">{trimmedSum(overdue.map((f) => f.total))}</strong> · My
      due total <strong data-testid="my-due-total">{mine === undefined ? '' : dueTotals.get(mine.id) ?? ''}</strong> · My
      overdue total <strong data-testid="my-overdue-total">{mine === undefined ? '' : overdueById.get(mine.id)?.total ?? ''}</strong>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -10, Component: ({ sheetId }) => <Header sheetId={sheetId} /> });

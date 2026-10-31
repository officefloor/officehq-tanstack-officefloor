import { SheetPanels } from '../../slots/defs/sheetPanels';
import { STATUSES, useClaims } from '../claims/useClaims';
import { showDue } from '../rates/useRates';
import { useSheetTotals } from '../claims/useSheetTotals';
import { usePeople } from '../people/usePeople';

/**
 * The sheet's header, shown at all times: how many people are on the sheet (retired ones
 * included), how many claims have each status (as the summary counts them), and the sheet's
 * due-total and overdue-total (as the totals work them out). And the signed-in user's own figures:
 * the due-total and overdue-total of the person whose name or an alias is the user's name (as names
 * are compared: case and the spaces around it ignored); blank when there is no such person.
 */
function SheetHeader({ user, sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: everyone = [] } = usePeople(sheet);
  const { dueTotal, overdueTotal, perPerson } = useSheetTotals(sheet);
  const key = (n: string) => n.trim().toLowerCase();
  const me = perPerson.find(({ person }) => [person.name, ...person.aliases].some((n) => key(n) === key(user)));
  return (
    <header data-testid="sheet-header">
      <span>
        People: <strong data-testid="header-people-count">{everyone.length}</strong>
      </span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}
          {s}: <strong data-testid={`header-claims-${s}-count`}>{all.filter((c) => c.status === s).length}</strong>
        </span>
      ))}
      <span>
        {' '}
        Due: <strong data-testid="header-due-total">{dueTotal}</strong>
      </span>
      <span>
        {' '}
        Overdue: <strong data-testid="header-overdue-total">{overdueTotal}</strong>
      </span>
      <span>
        {' '}
        My due: <strong data-testid="my-due-total">{me ? showDue(me.due) : ''}</strong>
      </span>
      <span>
        {' '}
        My overdue: <strong data-testid="my-overdue-total">{me ? showDue(me.overdue) : ''}</strong>
      </span>
    </header>
  );
}

export const contribution = SheetPanels.fill({ order: -100, Component: SheetHeader });

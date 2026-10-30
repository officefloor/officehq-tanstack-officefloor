import { asString, useSearchParam } from '../../url/useSearchParam';
import { showDue } from '../rates/due';
import { STATUSES, useSheetFigures } from '../people/useSheetFigures';

// The sheet's counters, always on show: its people (retired included), its claims per status (as
// the summary counts them) and its due-total and overdue-total (as the sheet's totals show them).
// Then the signed-in user's own figures: the due-total and overdue-total of the person on the sheet
// with the user's name or alias (case and outer spaces ignored); nothing when there is no such person.
export function SheetHeader() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const { people, sheetDueTotal, sheetOverdueTotal, statusCount, totalOf, overdue, overdueTotal } = useSheetFigures(sheet);
  const key = (name: string) => name.trim().toLowerCase();
  const me = user === '' ? undefined : people.find((p) => [p.name, ...p.aliases].some((n) => key(n) === key(user)));
  return (
    <header data-testid="header">
      People: <span data-testid="header-people-count">{people.length}</span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s}: <span data-testid={`header-claims-${s}-count`}>{statusCount(s)}</span>
        </span>
      ))}
      {' · '}Due: <span data-testid="header-due-total">{sheetDueTotal}</span>
      {' · '}Overdue: <span data-testid="header-overdue-total">{sheetOverdueTotal}</span>
      {' · '}Mine: due <span data-testid="my-due-total">{me ? showDue(totalOf(me)) : ''}</span>
      {', '}overdue <span data-testid="my-overdue-total">{me ? overdueTotal(overdue.filter((c) => c.personId === me.id)) : ''}</span>
    </header>
  );
}

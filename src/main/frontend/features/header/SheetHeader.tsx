import { asString, useSearchParam } from '../../url/useSearchParam';
import { formatCents, showDue } from '../rates/due';
import { crewDueCents, useCrews } from '../crews/useCrews';
import { useClaims } from '../claims/useClaims';
import { STATUSES, useSheetFigures } from '../people/useSheetFigures';

// The sheet's counters, always on show: its people (retired included), its claims per status (as
// the summary counts them) and its due-total and overdue-total (as the sheet's totals show them).
// Then the signed-in user's own figures: the due-total and overdue-total of the person on the sheet
// with the user's name or alias (case and outer spaces ignored); nothing when there is no such person.
// And the user's crew total: the due-totals of the crews that person leads, added up; nothing when
// they lead none. And the count of REVIEW claims assigned to the user for review.
export function SheetHeader() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const { people, byId, sheetDueTotal, sheetOverdueTotal, statusCount, totalOf, overdue, overdueTotal } = useSheetFigures(sheet);
  const crews = useCrews(sheet);
  const claims = useClaims(sheet);
  const myReviews = claims.filter((c) => c.status === 'REVIEW' && user.trim() !== '' && (c.reviewUser ?? '') === user.trim()).length;
  const key = (name: string) => name.trim().toLowerCase();
  const me = user === '' ? undefined : people.find((p) => [p.name, ...p.aliases].some((n) => key(n) === key(user)));
  const led = me ? crews.filter((c) => c.leadId === me.id) : [];
  const crewTotal = led.length === 0 ? '' : formatCents(led.reduce((sum, c) => sum + crewDueCents(c, byId, totalOf), 0n));
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
      {', '}crew due <span data-testid="my-crew-due-total">{crewTotal}</span>
      {', '}reviews <span data-testid="my-reviews-count">{myReviews}</span>
    </header>
  );
}

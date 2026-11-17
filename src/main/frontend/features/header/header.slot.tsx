import { SheetPage } from '../../slots/defs/sheetPage';
import { usePeople } from '../people/people';
import { STATUSES, useSheetDueTotal, useSheetOverdue, useMyCrewDue, useMyFigures, useMyReviewsCount, useStatusCounts, useWatchedDue } from './totals';

/** The sheet's header counters, at the top of the sheet: its people (retired included), its claims per status
 *  (as the summary counts them), its due-total and overdue-total (as the rollups show them), and the signed-in user's
 *  own due-total and overdue-total (blank when no person on the sheet has the user's name or alias) and crew total
 *  (blank unless that person leads a crew), how many REVIEW claims are assigned to the user for review, and the
 *  due-totals of the people the user watches added up (0.00 when watching no one). */
function Header({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const withStatus = useStatusCounts(sheetId);
  const dueTotal = useSheetDueTotal(sheetId);
  const overdue = useSheetOverdue(sheetId);
  const my = useMyFigures(sheetId, user);
  const myCrew = useMyCrewDue(sheetId, user);
  const myReviews = useMyReviewsCount(sheetId, user);
  const watchedDue = useWatchedDue(sheetId, user);

  return (
    <header data-testid="header">
      People <span data-testid="header-people-count">{(people.data ?? []).length}</span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s} <span data-testid={`header-claims-${s}-count`}>{withStatus(s)}</span>
        </span>
      ))}
      {' · '}Due total <span data-testid="header-due-total">{dueTotal}</span>
      {' · '}Overdue total <span data-testid="header-overdue-total">{overdue.total}</span>
      {' · '}My due total <span data-testid="my-due-total">{my?.due ?? ''}</span>
      {' · '}My overdue total <span data-testid="my-overdue-total">{my?.overdue ?? ''}</span>
      {' · '}My crew due total <span data-testid="my-crew-due-total">{myCrew ?? ''}</span>
      {' · '}My reviews <span data-testid="my-reviews-count">{myReviews}</span>
      {' · '}Watched due total <span data-testid="watched-due-total">{watchedDue}</span>
    </header>
  );
}

export const contribution = SheetPage.fill({ order: -10, Component: Header });

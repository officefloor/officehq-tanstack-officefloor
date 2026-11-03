import { SheetBody } from '../../slots/defs/sheetBody';
import { crewDue } from '../crews/crewDue';
import { useCrews } from '../crews/useCrews';
import { useClaims } from '../claims/useClaims';
import { useOverdue } from '../overdue/useOverdue';
import { useAliases, usePeople, useWatched } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { sumDues, trimmedSum } from '../rates/useRates';
import { useSignedInUser } from '../session/useSession';
import { countWithStatus, STATUSES } from '../summary/statusCounts';
import { useFigure } from '../locale/useLocale';

/** The sheet's header counters, shown at all times: how many people are on the sheet (retired people
 *  included), how many claims have each status (as the summary counts them), and the sheet's
 *  due-total and overdue-total (as the totals show them, trimmed); and the signed-in user's own figures: the
 *  due-total and overdue-total of the person whose name or an alias is the user's name (ignoring case), blank
 *  when there is no such person; and their crew total: the due-totals of the crews that person leads, added
 *  up, blank when they lead none; how many REVIEW claims are assigned to them to review; and their watched
 *  total: the due-totals of the people they watch, added up (0.00 when they watch no one). */
function Header({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
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
  const crews = useCrews(sheetId);
  const led = mine === undefined ? [] : (crews.data ?? []).filter((c) => c.leadId === mine.id);
  const myReviews = me === '' ? 0 : (claims.data ?? []).filter((c) => c.status === 'REVIEW'
    && (c.reviewAssigned ?? '').trim().toLowerCase() === me).length;
  const watched = useWatched(sheetId);
  const watchedDue = sumDues((watched.data ?? []).map((id) => dueTotals.get(id) ?? 'n/a'));
  const myCrewDue = led.length === 0 ? '' : sumDues(led.map((c) => crewDue(c, dueTotals)));
  return (
    <header data-testid="header">
      People <strong data-testid="header-people-count">{(people.data ?? []).length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}
          · {s} <strong data-testid={`header-claims-${s}-count`}>{countWithStatus(claims.data, s)}</strong>
        </span>
      ))}{' '}
      · Due total <strong data-testid="header-due-total">{fig(trimmedSum([...dueTotals.values()]))}</strong> · Overdue
      total <strong data-testid="header-overdue-total">{fig(trimmedSum(overdue.map((f) => f.total)))}</strong> · My
      due total <strong data-testid="my-due-total">{fig(mine === undefined ? '' : dueTotals.get(mine.id) ?? '')}</strong> · My
      overdue total <strong data-testid="my-overdue-total">{fig(mine === undefined ? '' : overdueById.get(mine.id)?.total ?? '')}</strong> · My
      crew due total <strong data-testid="my-crew-due-total">{fig(myCrewDue)}</strong> · My reviews{' '}
      <strong data-testid="my-reviews-count">{myReviews}</strong> · Watched due total{' '}
      <strong data-testid="watched-due-total">{fig(watchedDue)}</strong>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -10, Component: ({ sheetId }) => <Header sheetId={sheetId} /> });

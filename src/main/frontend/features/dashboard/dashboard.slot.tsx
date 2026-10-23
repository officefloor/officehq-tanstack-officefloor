import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { overdueDues, type Rate } from '../rates/rates';
import { useActingRole } from '../roles/roles.slot';
import { floorOf, lateOf, type Settings } from '../settings/settings';
import { useAsOf, usePreview, type Sheet } from '../sheet/sheet';

/** One read of a sheet as the store holds it now (given a preview date, figured on it). Keyed under the sheet, so a
 *  load of that sheet re-reads it. */
function useNow<T>(sheetId: string, part: string, preview = '') {
  return useQuery({
    queryKey: ['sheet', sheetId, 'now', part, '', preview],
    queryFn: () => getJsonNow<T>(`/api/sheets/${encodeURIComponent(sheetId)}${part === '' ? '' : `/${part}`}`
      + (preview === '' ? '' : `?preview=${encodeURIComponent(preview)}`)),
  });
}

/** The tree's counts gathered so far. */
type Counts = { pending: number; review: number; overdue: number };

const NONE: Counts = { pending: 0, review: 0, overdue: 0 };

/** Adds the counts of sheets[at] to `sum`, then goes on to the next sheet; past the last, shows the counts added
 *  up. One component per sheet keeps each sheet's reads in hooks of its own. */
function Accumulate({ sheets, at, sum, steward, preview }: {
  sheets: string[]; at: number; sum: Counts; steward: boolean; preview: string;
}) {
  if (at >= sheets.length) return <Shown counts={sum} steward={steward} />;
  return <SheetCounts key={sheets[at]} sheets={sheets} at={at} sum={sum} steward={steward} preview={preview} />;
}

/** A sheet's counts as it shows them now: its PENDING claims, its REVIEW claims, and its overdue claims that count
 *  (as its overdue rollup counts them, with its own today and settings; the preview date instead, given one). */
function SheetCounts({ sheets, at, sum, steward, preview }: {
  sheets: string[]; at: number; sum: Counts; steward: boolean; preview: string;
}) {
  const sheetId = sheets[at];
  const people = useNow<Person[]>(sheetId, 'people').data ?? [];
  const claims = useNow<Claim[]>(sheetId, 'claims', preview).data ?? [];
  const rates = useNow<Rate[]>(sheetId, 'rates').data ?? [];
  const settings = useNow<Settings>(sheetId, 'settings').data;
  const sheetToday = useNow<Sheet>(sheetId, '').data?.today ?? '';
  const today = preview !== '' && sheetToday !== '' ? preview : sheetToday;
  const late = lateOf(settings, today, claims, people);
  const next: Counts = {
    pending: sum.pending + claims.filter((c) => c.status === 'PENDING').length,
    review: sum.review + claims.filter((c) => c.status === 'REVIEW').length,
    overdue: sum.overdue + overdueDues(claims, people, rates, late, floorOf(settings)).length,
  };
  return <Accumulate sheets={sheets} at={at + 1} sum={next} steward={steward} preview={preview} />;
}

function Shown({ counts, steward }: { counts: Counts; steward: boolean }) {
  const show = (n: number) => (steward ? String(n) : '');
  return (
    <section data-testid="dashboard">
      <h2>Steward's dashboard</h2>
      <table>
        <tbody>
          <tr>
            <th>Pending</th>
            <td data-testid="dash-pending-count">{show(counts.pending)}</td>
          </tr>
          <tr>
            <th>Review</th>
            <td data-testid="dash-review-count">{show(counts.review)}</td>
          </tr>
          <tr>
            <th>Overdue</th>
            <td data-testid="dash-overdue-count">{show(counts.overdue)}</td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}

/** The steward's dashboard: over the sheet and all its descendants (its children — the sheets naming it as their
 *  parent — theirs, and so on), as the store holds them now, how many claims are PENDING, how many REVIEW, and how
 *  many overdue claims count (as each sheet's overdue rollup counts them). Anyone not acting as a steward sees the
 *  cells empty, and so does everyone while the screen shows the sheet as of a date. */
function Dashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const [asOf] = useAsOf();
  const [preview] = usePreview();
  const steward = useActingRole(sheetId, user) === 'steward' && !asOf;
  const descendants = useNow<string[]>(sheetId, 'descendants').data ?? [];
  return <Accumulate sheets={[sheetId, ...descendants]} at={0} sum={NONE} steward={steward} preview={preview} />;
}

export const contribution = SheetBody.fill({ order: 21, Component: Dashboard });

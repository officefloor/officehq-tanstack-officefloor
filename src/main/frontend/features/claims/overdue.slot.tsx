import { HomeMain } from '../../slots/defs/homeMain';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { usePeople } from '../people/people';
import { show, usePersonRate } from '../rates/rates';
import { useCurrentSheet } from '../session/session';
import { useClaimDue, useClaims, useCounts, useOverdue } from './claims';

function OverdueColumns() {
  return (
    <>
      <th>Overdue</th>
      <th>Overdue total</th>
    </>
  );
}

/** A person's overdue claims that count: how many, and their dues added up in cents (n/a as 0). */
function useOverdueOf() {
  const claims = useClaims().data ?? [];
  const counts = useCounts();
  const overdue = useOverdue();
  const rateOf = usePersonRate();
  const claimDue = useClaimDue();
  return (personId: number) => {
    const late = claims.filter((c) => c.personId === personId && counts(c) && overdue(c));
    const due = late.reduce((n, c) => n + (claimDue(c, rateOf(personId, c.date)) ?? 0n), 0n);
    return { count: late.length, due };
  };
}

// A person's overdue count and overdue-total: their OPEN claims that count, due by before the sheet's today.
function PersonOverdue({ personId }: { personId: number }) {
  const { count, due } = useOverdueOf()(personId);
  return (
    <>
      <td data-testid="person-overdue-count">{count}</td>
      <td data-testid="person-overdue-total">{show(due)}</td>
    </>
  );
}

// The sheet's overdue count and overdue-total: everyone's added up.
function OverdueTotal() {
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const overdueOf = useOverdueOf();
  if (!sheet) return null;
  const all = people.map((p) => overdueOf(p.id));
  return (
    <section data-testid="rollup-overdue">
      Overdue <strong data-testid="rollup-overdue-count">{all.reduce((n, o) => n + o.count, 0)}</strong>{' '}
      Overdue total <strong data-testid="rollup-overdue-total">{show(all.reduce((n, o) => n + o.due, 0n))}</strong>
    </section>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 30, Component: OverdueColumns }),
  PersonCells.fill({ order: 30, Component: PersonOverdue }),
  HomeMain.fill({ order: 22, Component: OverdueTotal }),
];

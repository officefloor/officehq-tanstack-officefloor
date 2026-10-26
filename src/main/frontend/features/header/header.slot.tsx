import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { useCrews } from '../crews/crews.slot';
import { useOverdue } from '../overdue/overdue.slot';
import { usePeople } from '../people/people.slot';
import { NA, useDueTotals, useSheetDueTotal } from '../rates/rates.slot';
import { showHundredths } from '../rates/useRates';
import { STATUSES } from '../summary/summary.slot';

// The header's counters, at the top of the sheet: the people on it (retired people included), the claims in each
// status (as the summary counts them), and the sheet's due-total and overdue-total (as the rollups show them). Then the
// signed-in user's own figures: the due-total and overdue-total of the person whose name or alias is the user's name (as
// names are compared: trimmed, ignoring case); blank when no one on the sheet is. And the user's crew total: the
// due-totals of the crews that person leads (as the crews show them), added up; blank when they lead none.
function SheetHeader({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const due = useSheetDueTotal(sheetId);
  const overdueAll = useOverdue(sheetId);
  const overdue = overdueAll.sheet;
  const dueTotals = useDueTotals(sheetId);
  const key = user.trim().toLowerCase();
  const me = (people.data ?? []).find((p) => [p.name, ...(p.aliases ?? [])].some((n) => n.trim().toLowerCase() === key));
  const myDue = me == null ? '' : (() => { const t = dueTotals.get(me.id); return t == null ? NA : showHundredths(t); })();
  const myOverdue = me == null ? '' : showHundredths(overdueAll.byPerson.get(me.id)?.cents ?? 0n);
  const crews = useCrews(sheetId);
  const myCrews = me == null ? [] : (crews.data ?? []).filter((c) => c.lead.personId === me.id);
  let myCrewTotal = 0n;
  for (const c of myCrews) for (const p of [c.lead, ...c.members]) myCrewTotal += dueTotals.get(p.personId) ?? 0n;
  const myCrewDue = myCrews.length === 0 ? '' : showHundredths(myCrewTotal);
  return (
    <header data-testid="sheet-header">
      <p>
        People: <strong data-testid="header-people-count">{(people.data ?? []).length}</strong>{' '}
        {STATUSES.map((s) => (
          <span key={s}>
            {s}: <strong data-testid={`header-claims-${s}-count`}>{(claims.data ?? []).filter((c) => c.status === s).length}</strong>{' '}
          </span>
        ))}
        Due total: <strong data-testid="header-due-total">{showHundredths(due)}</strong>{' '}
        Overdue total: <strong data-testid="header-overdue-total">{showHundredths(overdue.cents)}</strong>
      </p>
      <p>
        My due total: <strong data-testid="my-due-total">{myDue}</strong>{' '}
        My overdue total: <strong data-testid="my-overdue-total">{myOverdue}</strong>{' '}
        My crew due total: <strong data-testid="my-crew-due-total">{myCrewDue}</strong>
      </p>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -10, Component: SheetHeader });

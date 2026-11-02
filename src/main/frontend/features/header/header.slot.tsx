import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { showDue, twoDecimals, useRates } from '../rates/rates';
import { useAliases } from '../aliases/aliases';
import { useSheetToday } from '../sheet/sheet';
import { useCapCents, useFloorCents } from '../settings/settings';
import {
  CLAIM_STATUSES,
  overdueCents,
  personDueCents,
  statusCount,
  useClaims,
  useLate,
  useSheetTotals,
} from '../claims/claims';

// The signed-in user's own figures: the due-total and overdue-total of the person whose name or an alias is the
// user's name (as names are compared: without regard to case), as their row shows them; none when there is none.
function useMyTotals(sheet: string, user: string): { due: string; overdue: string } {
  const { data: people = [] } = usePeople(sheet);
  const { data: aliases = [] } = useAliases(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const cap = useCapCents(sheet);
  const today = useSheetToday(sheet);
  const late = useLate(sheet);
  const me = user.toLowerCase();
  const person =
    people.find((p) => p.name.toLowerCase() === me) ??
    people.find((p) => aliases.some((a) => a.person === p.id && a.alias.toLowerCase() === me));
  if (person === undefined) return { due: '', overdue: '' };
  const mine = claims.filter((c) => c.person === person.id);
  return {
    due: showDue(personDueCents(mine, rates, person, today, floor, late, cap)),
    overdue: twoDecimals(overdueCents(mine, [person], rates, late, floor).cents),
  };
}

// The sheet's counters, shown at all times above everything else. Each repeats a figure figured elsewhere, from the
// same helpers: the people on the sheet (retired ones included), the claims per status (as the summary counts them),
// the sheet's due-total and overdue-total (as the rollup shows them), and the user's own figures (see useMyTotals).
function Header({ sheet, user }: { sheet: string; user: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { due, overdue } = useSheetTotals(sheet);
  const my = useMyTotals(sheet, user);
  return (
    <header>
      <span>
        People: <strong data-testid="header-people-count">{people.length}</strong>
      </span>
      {CLAIM_STATUSES.map((s) => (
        <span key={s}>
          {' '}
          {s}: <strong data-testid={`header-claims-${s}-count`}>{statusCount(claims, s)}</strong>
        </span>
      ))}
      <span>
        {' '}
        Due total: <strong data-testid="header-due-total">{twoDecimals(due)}</strong>
      </span>
      <span>
        {' '}
        Overdue total: <strong data-testid="header-overdue-total">{twoDecimals(overdue.cents)}</strong>
      </span>
      <span>
        {' '}
        My due total: <strong data-testid="my-due-total">{my.due}</strong>
      </span>
      <span>
        {' '}
        My overdue total: <strong data-testid="my-overdue-total">{my.overdue}</strong>
      </span>
    </header>
  );
}

export const contribution = SheetPanels.fill({ order: -10, Component: Header });

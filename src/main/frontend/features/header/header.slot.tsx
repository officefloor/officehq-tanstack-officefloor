import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople, type Person } from '../people/people';
import { crewDueCents, useCrews } from '../crews/crews';
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
  usePersonDueCentsOf,
  useSheetTotals,
} from '../claims/claims';
import { Fig } from '../locale/Fig';

// The person who is the signed-in user: the one whose name or an alias is the user's name (as names are compared:
// without regard to case); none when there is none.
function useMe(sheet: string, user: string): Person | undefined {
  const { data: people = [] } = usePeople(sheet);
  const { data: aliases = [] } = useAliases(sheet);
  const me = user.toLowerCase();
  return (
    people.find((p) => p.name.toLowerCase() === me) ??
    people.find((p) => aliases.some((a) => a.person === p.id && a.alias.toLowerCase() === me))
  );
}

// The signed-in user's own figures: the due-total and overdue-total of the person who is the user (see useMe), as
// their row shows them; none when there is none.
function useMyTotals(sheet: string, user: string): { due: string; overdue: string } {
  const person = useMe(sheet, user);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const cap = useCapCents(sheet);
  const today = useSheetToday(sheet);
  const late = useLate(sheet);
  if (person === undefined) return { due: '', overdue: '' };
  const mine = claims.filter((c) => c.person === person.id);
  return {
    due: showDue(personDueCents(mine, rates, person, today, floor, late, cap)),
    overdue: twoDecimals(overdueCents(mine, [person], rates, late, floor).cents),
  };
}

// The signed-in user's crew total: the due-totals of the crews led by the person who is the user (see useMe), as
// their rows show them, added up; none when the user leads no crew.
function useMyCrewTotal(sheet: string, user: string): string {
  const person = useMe(sheet, user);
  const { data: people = [] } = usePeople(sheet);
  const { data: crews = [] } = useCrews(sheet);
  const dueOf = usePersonDueCentsOf(sheet);
  if (person === undefined) return '';
  const led = crews.filter((c) => c.lead.person === person.id);
  if (led.length === 0) return '';
  return twoDecimals(led.reduce((sum, c) => sum + crewDueCents(c, people, dueOf), 0n));
}

// The sheet's counters, shown at all times above everything else. Each repeats a figure figured elsewhere, from the
// same helpers: the people on the sheet (retired ones included), the claims per status (as the summary counts them),
// the sheet's due-total and overdue-total (as the rollup shows them), and the user's own figures (see useMyTotals and
// useMyCrewTotal).
function Header({ sheet, user }: { sheet: string; user: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { due, overdue } = useSheetTotals(sheet);
  const my = useMyTotals(sheet, user);
  const myCrew = useMyCrewTotal(sheet, user);
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
        Due total: <strong data-testid="header-due-total"><Fig>{twoDecimals(due)}</Fig></strong>
      </span>
      <span>
        {' '}
        Overdue total: <strong data-testid="header-overdue-total"><Fig>{twoDecimals(overdue.cents)}</Fig></strong>
      </span>
      <span>
        {' '}
        My due total: <strong data-testid="my-due-total"><Fig>{my.due}</Fig></strong>
      </span>
      <span>
        {' '}
        My overdue total: <strong data-testid="my-overdue-total"><Fig>{my.overdue}</Fig></strong>
      </span>
      <span>
        {' '}
        My crew due total: <strong data-testid="my-crew-due-total"><Fig>{myCrew}</Fig></strong>
      </span>
    </header>
  );
}

export const contribution = SheetPanels.fill({ order: -10, Component: Header });

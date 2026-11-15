import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey, type Person } from '../people/api';
import { useDueTotal } from '../rates/DueTotal';
import { claimDue, listRates, overdue, personDue, rateByGrade, ratesKey, showDue, sumDues } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { useOverdue } from '../rates/Overdue';
import { STATUSES, statusCount } from './Summary';
import { heldOf } from '../rates/shares';
import { Fig } from '../session/Fig';
import { crewDueTotal, crewsKey, listCrews } from '../crews/api';
import { useDueOf } from '../crews/Crews';

/** Names as they are compared: trimmed and lower-cased. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The signed-in user's own figures: the due-total and overdue-total, as their row shows them, of the
 *  person on the sheet whose name or an alias is the user's name (as names are compared); null when
 *  there is no such person. */
function useMyFigures(sheetId: string, user: string): { due: string; overdue: string } | null {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  const me = nameKey(user);
  const p = (people.data ?? []).find((q: Person) => [q.name, ...q.aliases].some((n) => nameKey(n) === me));
  if (!p || !claims.data) return null;
  const live = liveClaims(claims.data.filter((c) => c.personId === p.id), [p]);
  const dueOfClaim = (c: (typeof live)[number]) => claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges);
  const due = personDue(live, rates.data, (d) => gradeOn(p, d), rateByGrade(rates.data, today).get(p.grade), floor, charges, cap,
    heldOf(p.id, claims.data, people.data, rates.data, floor, charges));
  return { due: showDue(due), overdue: overdue(live, charges, dueOfClaim).total };
}

/** The signed-in user's crew total: the due-totals (see `crewDueTotal`) of the crews led by the person
 *  whose name or an alias is the user's name, added up; null when there is no such person or they lead no crew. */
function useMyCrewTotal(sheetId: string, user: string): string | null {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const crews = useQuery({ queryKey: crewsKey(sheetId), queryFn: () => listCrews(sheetId) });
  const dueOf = useDueOf(sheetId, user);
  const me = nameKey(user);
  const p = (people.data ?? []).find((q: Person) => [q.name, ...q.aliases].some((n) => nameKey(n) === me));
  const led = (crews.data ?? []).filter((c) => p && c.leadId === p.id);
  if (led.length === 0) return null;
  return sumDues(led.map((c) => crewDueTotal(c, dueOf)));
}

/** The sheet's header counters, shown at all times: everyone on the sheet (retired included), the
 *  claims per status (as the summary counts them), the sheet's due- and overdue-totals, and the
 *  signed-in user's own (blank when no one on the sheet has their name) and their crew total (blank when
 *  they lead no crew). */
export function Header({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const dueTotal = useDueTotal(sheetId, user);
  const late = useOverdue(sheetId, user);
  const mine = useMyFigures(sheetId, user);
  const myCrew = useMyCrewTotal(sheetId, user);
  return (
    <header>
      People <span data-testid="header-people-count">{(people.data ?? []).length}</span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}{s} <span data-testid={`header-claims-${s}-count`}>{statusCount(claims.data, s)}</span>
        </span>
      ))}
      {' · '}Due <span data-testid="header-due-total"><Fig>{dueTotal}</Fig></span>
      {' · '}Overdue <span data-testid="header-overdue-total"><Fig>{late.total}</Fig></span>
      {' · '}My due <span data-testid="my-due-total"><Fig>{mine?.due ?? ''}</Fig></span>
      {' · '}My overdue <span data-testid="my-overdue-total"><Fig>{mine?.overdue ?? ''}</Fig></span>
      {' · '}My crews due <span data-testid="my-crew-due-total"><Fig>{myCrew ?? ''}</Fig></span>
    </header>
  );
}

import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { PersonError } from './personError';
import { useClaims } from '../claims/claims';
import { NA, personDue, useRates } from '../rates/rates';
import { useDueRules, useSettings } from '../settings/settings';
import { useCrews } from '../crews/crews';
import { useAliases } from '../aliases/aliases';
import { useSheetToday } from '../session/sheetToday';
import { personNamed } from '../counters/HeaderCounters';
import { liveHolds, nameKey, shownName, useEditingPerson, usePeople, usePeopleSort, useShowRetired } from './people';
import type { Person, SortKey } from './people';
import { ColumnHead } from '../columns/ColumnHead';
import { useColumnShown } from '../columns/columns';

/** Orders a and b by key: negative, zero or positive. */
const compare = (a: string | number, b: string | number) => (a < b ? -1 : a > b ? 1 : 0);

// Every person on the sheet, in their order in the list (their positions) or sorted as this screen chose, each with their name and grade and
// the columns features add (slots/defs/personColumns.ts). Retired people are left out unless the
// show-retired toggle is on. A restricted user sees only the people of the crews they lead or belong to and themselves.
export function PeopleList() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setEditing] = useEditingPerson();
  const people = usePeople();
  const [showRetired] = useShowRetired();
  const [sort] = usePeopleSort();
  const sortKey = useSortKey(sort);
  const showTerms = useColumnShown('terms');
  const showContact = useColumnShown('contact');
  const shown = useShownToUser();

  if (!user || !sheet) return null;

  return (
    <table data-testid="people">
      <thead>
        <tr>
          <th>Name</th>
          <th>Grade</th>
          <ColumnHead column="terms">Terms</ColumnHead>
          <ColumnHead column="contact">Contact</ColumnHead>
          <th>Clashing</th>
          <PersonColumnHeads.Slot />
          <th />
        </tr>
      </thead>
      <tbody>
        {sorted((people.data ?? []).filter((p) => (showRetired || !p.retired) && shown(p)), sortKey).map((p) => (
          <tr key={p.id} data-testid={`person-row-${p.id}`}>
            <td data-testid="person-name" style={{ whiteSpace: 'pre' }}>
              {shownName(p)}
            </td>
            <td data-testid="person-grade">{p.grade}</td>
            {showTerms && <td data-testid="person-terms">{p.terms ?? ''}</td>}
            {showContact && <td data-testid="person-contact">{p.contact ?? ''}</td>}
            <td data-testid="person-clashing">{p.clashing ? 'yes' : 'no'}</td>
            <PersonColumns.Slot person={p} />
            <td>
              <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                Edit
              </button>
              <PersonError personId={p.id} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/**
 * Whether the signed-in user may see a person on the list: anyone, unless their role is restricted (and no delegation in
 * force on the sheet's today makes them a steward), when only the person with their name (by name or alias) and the
 * leads and members of the crews that person leads or belongs to. Nothing is shown until that is known.
 */
function useShownToUser(): (p: Person) => boolean {
  const [user] = useCurrentUser();
  const settings = useSettings();
  const crews = useCrews();
  const aliases = useAliases();
  const people = usePeople().data ?? [];
  const today = useSheetToday();
  if (!settings.data) return () => false;
  const restricted =
    settings.data.roles.some((r) => r.user === user && r.role === 'restricted') &&
    !settings.data.delegations.some((d) => d.user === user && d.from <= today && today <= d.to);
  if (!restricted) return () => true;
  if (!crews.data || !aliases.data) return () => false;
  const me = personNamed(people, aliases.data, user ?? '');
  if (!me) return () => false;
  const visible = new Set<number>([me.id]);
  for (const crew of crews.data) {
    const all = [...(crew.lead ? [crew.lead] : []), ...crew.members];
    if (all.some((m) => m.id === me.id)) all.forEach((m) => visible.add(m.id));
  }
  return (p) => visible.has(p.id);
}

/** The people sorted by their key, ascending; equal keys (and no key) keep list order. */
function sorted(people: Person[], key: ((p: Person) => string | number) | null): Person[] {
  if (!key) return people;
  return people.map((p, i) => ({ p, i, k: key(p) })).sort((a, b) => compare(a.k, b.k) || a.i - b.i).map((e) => e.p);
}

/** The key a person sorts on: their name as names compare, their grade, or their due-total (n/a last); null: list order. */
function useSortKey(sort: SortKey): ((p: Person) => string | number) | null {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = useClaims().data ?? [];
  const people = usePeople().data ?? [];
  switch (sort) {
    case 'name':
      return (p) => nameKey(p.name);
    case 'grade':
      return (p) => p.grade;
    case 'due-total':
      return (p) => {
        const total = personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules, {
          personId: p.id,
          claims,
          people,
        });
        return total === NA ? Infinity : Math.round(Number(total) * 100);
      };
    default:
      return null;
  }
}

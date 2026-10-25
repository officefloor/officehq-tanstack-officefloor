import { useEffect } from 'react';
import { useIsFetching, useIsMutating } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { useClaims } from '../claims/claims';
import { personDue } from '../rates/due.slot';
import { useRates } from '../rates/rates';
import { useDelegations, useRoles, useSettings, type Delegation, type Role } from '../roles/roles';
import { useCrews, type Crew } from '../crews/crews';
import { roleOf } from '../header/dashboard.slot';
import { personNamed } from '../header/counters.slot';
import { usePools } from '../pools/pools';
import {
  gradeOn, keepQuickAddedFor, nameKey, shownName, useQuickAdded, peopleQueryKey, personSaveKey, sortedBy, usePeople, useEditingPerson, useShowRetired, useSortPeople,
  type Person,
} from './people';

/** Whether the user sees a person on the people list: anyone, unless they act as restricted — then only the person
 *  going by their name and the leads and members of the crews that person leads or belongs to (no one while the
 *  roles, delegations or crews are not yet read). */
function visibleTo(people: Person[], roles: Role[] | undefined, delegations: Delegation[] | undefined,
  crews: Crew[] | undefined, user: string, today: string | undefined): (p: Person) => boolean {
  if (roles === undefined || delegations === undefined) return () => false;
  if (roleOf(roles, delegations, user, today) !== 'restricted') return () => true;
  const me = personNamed(people, user);
  if (!me || crews === undefined) return () => false;
  const ids = new Set<number>([me.id]);
  for (const c of crews) {
    if (c.lead?.id === me.id || c.members.some((m) => m.id === me.id)) {
      if (c.lead) ids.add(c.lead.id);
      c.members.forEach((m) => ids.add(m.id));
    }
  }
  return (p) => ids.has(p.id);
}

// The sheet's people in list order — or as the sort select shows them — — retired people only while show-retired is on — each with their name (shown as <name> (<grade>)), grade in force on the sheet's today and an edit button (disabled while a person save is on its way, or the people are being read again);
// features add their own columns through PeopleColumns / PersonCells. A restricted user (in the role they act in)
// sees only the person going by their name and the people of the crews that person leads or belongs to (none until
// the roles and crews are read), and the people this screen quick-added since it loaded the sheet; every other figure
// stays over everyone.
function PeopleList() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setEditing] = useEditingPerson();
  const people = usePeople();
  const [showRetired] = useShowRetired();
  const today = useSheetToday();
  const [sort] = useSortPeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const pools = usePools();
  const roles = useRoles();
  const delegations = useDelegations();
  const crews = useCrews();
  const saving = useIsMutating({ mutationKey: personSaveKey }) > 0;
  const reading = useIsFetching({ queryKey: peopleQueryKey(sheet ?? '') }) > 0;
  const quickAdded = useQuickAdded(sheet);
  useEffect(() => keepQuickAddedFor(sheet ?? ''), [sheet]);
  if (!user || !sheet) return null;
  const visible = visibleTo(people.data ?? [], roles.data, delegations.data, crews.data, user, today);
  const listed = (people.data ?? []).filter((p) => (visible(p) || quickAdded.includes(p.id)) && (showRetired || p.retiredFrom === null));
  const due = (p: Person) => {
    const d = personDue(p, claims.data ?? [], rates.data, today, settings.data, people.data, pools.data);
    return d === 'n/a' ? null : Number(d);
  };
  const shown = sort === 'name' ? sortedBy(listed, (p) => nameKey(p.name))
    : sort === 'grade' ? sortedBy(listed, (p) => gradeOn(p, today))
    : sort === 'due-total' ? sortedBy(listed, due)
    : listed;
  return (
    <table data-testid="people">
      <thead>
        <tr>
          <th>Name</th>
          <th>Grade</th>
          <th />
          <PeopleColumns.Slot />
        </tr>
      </thead>
      <tbody>
        {shown.map((p) => (
          <tr key={p.id} data-testid={`person-row-${p.id}`}>
            <td data-testid="person-name" style={{ whiteSpace: 'pre' }}>{shownName(p, today)}</td>
            <td data-testid="person-grade">{gradeOn(p, today)}</td>
            <td>
              <button type="button" data-testid={`person-edit-${p.id}`} disabled={saving || reading}
                onClick={() => setEditing(String(p.id))}>
                Edit
              </button>
            </td>
            <PersonCells.Slot person={p} />
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = HomeMain.fill({ order: 20, Component: PeopleList });

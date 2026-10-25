import { useIsFetching, useIsMutating } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { useClaims } from '../claims/claims';
import { personDue } from '../rates/due.slot';
import { useRates } from '../rates/rates';
import { useSettings } from '../roles/roles';
import {
  gradeOn, nameKey, shownName, peopleQueryKey, personSaveKey, sortedBy, usePeople, useEditingPerson, useShowRetired, useSortPeople,
  type Person,
} from './people';

// The sheet's people in list order — or as the sort select shows them — — retired people only while show-retired is on — each with their name (shown as <name> (<grade>)), grade in force on the sheet's today and an edit button (disabled while a person save is on its way, or the people are being read again);
// features add their own columns through PeopleColumns / PersonCells.
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
  const saving = useIsMutating({ mutationKey: personSaveKey }) > 0;
  const reading = useIsFetching({ queryKey: peopleQueryKey(sheet ?? '') }) > 0;
  if (!user || !sheet) return null;
  const listed = (people.data ?? []).filter((p) => showRetired || p.retiredFrom === null);
  const due = (p: Person) => {
    const d = personDue(p, claims.data ?? [], rates.data, today, settings.data);
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

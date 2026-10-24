import { useIsFetching, useIsMutating } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, peopleQueryKey, personSaveKey, usePeople, useEditingPerson, useShowRetired } from './people';

// The sheet's people in list order — retired people only while show-retired is on — each with their name, grade in force on the sheet's today and an edit button (disabled while a person save is on its way, or the people are being read again);
// features add their own columns through PeopleColumns / PersonCells.
function PeopleList() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setEditing] = useEditingPerson();
  const people = usePeople();
  const [showRetired] = useShowRetired();
  const today = useSheetToday();
  const saving = useIsMutating({ mutationKey: personSaveKey }) > 0;
  const reading = useIsFetching({ queryKey: peopleQueryKey(sheet ?? '') }) > 0;
  if (!user || !sheet) return null;
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
        {(people.data ?? []).filter((p) => showRetired || p.retiredFrom === null).map((p) => (
          <tr key={p.id} data-testid={`person-row-${p.id}`}>
            <td data-testid="person-name" style={{ whiteSpace: 'pre' }}>{p.name}</td>
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

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES, peopleKey, usePeople, type Person } from './usePeople';

/** The sheet's people: one form to add a person or edit the one whose editor is open, and the list. */
function PeoplePanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const [editing, setEditing] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const [blank, setBlank] = useState(false);
  const base = `/api/sheets/${encodeURIComponent(sheet)}/people`;

  const save = useMutation({
    mutationFn: (p: { id: number | null; name: string; grade: string }) =>
      p.id === null
        ? postJson<Person>(base, { name: p.name, grade: p.grade, user })
        : putJson<Person>(`${base}/${p.id}`, { name: p.name, grade: p.grade, user }),
    onSuccess: async () => {
      setEditing(null);
      setName('');
      setGrade('A');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again, or an editor is opened.
  const clearError = () => {
    save.reset();
    setBlank(false);
  };

  const open = (p: Person) => {
    clearError();
    setEditing(p.id);
    setName(p.name);
    setGrade(p.grade);
  };
  const cancel = () => {
    clearError();
    setEditing(null);
    setName('');
    setGrade('A');
  };

  return (
    <section data-testid="people-panel">
      <h2>People</h2>
      <form
        data-testid="person-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (name.trim() === '') {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            save.reset();
            setBlank(true);
            return;
          }
          setBlank(false);
          save.mutate({ id: editing, name, grade });
        }}
      >
        <label>
          Name{' '}
          <input
            data-testid="person-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              clearError();
            }}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add' : 'Save'}
        </button>
        {editing !== null && (
          <>
            {' '}
            <button type="button" data-testid="person-form-cancel" onClick={cancel}>
              Cancel
            </button>
          </>
        )}
        {(blank || save.isError) && (
          <span data-testid="person-form-error" role="alert">
            {' '}
            {blank ? 'A name is required.' : 'Could not save this person.'}
          </span>
        )}
      </form>
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th />
            <PersonColumns.Slot user={user} sheet={sheet} />
          </tr>
        </thead>
        <tbody>
          {people.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                  Edit
                </button>
              </td>
              <PersonCells.Slot user={user} sheet={sheet} person={p} />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 10, Component: PeoplePanel });

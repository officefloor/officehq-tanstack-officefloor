import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';

export type Person = { id: number; name: string; grade: string };

const GRADES = ['A', 'B', 'C'] as const;

/** The key every feature reads the sheet's people under. */
export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
  });
}

/** Which person's editor is open (search param `editPerson`); none means the form adds. */
export function useEditingPerson() {
  return useSearchParam('editPerson', asNumber);
}

// The person form: adds a person, or — with a person's editor open — saves their changes. It is
// re-mounted for each person opened (see PeoplePanel), so it starts filled with their values.
function PersonForm({ sheetId, editing }: { sheetId: string; editing?: Person }) {
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
  const save = useMutation({
    mutationFn: async (body: { name: string; grade: string }) => {
      if (body.name.trim() === '') throw new Error('Enter a name');
      return editing
        ? putJson<Person>(`${base}/${editing.id}`, body)
        : postJson<Person>(base, body);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      if (editing) {
        setEditing(undefined);
      } else {
        setName('');
        setGrade('A');
      }
    },
  });

  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        save.mutate({ name, grade });
      }}
    >
      <label>
        Name{' '}
        <input
          data-testid="person-form-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            save.reset();
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
            save.reset();
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
        {editing ? 'Save' : 'Add person'}
      </button>
      {editing && (
        <>
          {' '}
          <button type="button" data-testid="person-form-cancel" onClick={() => setEditing(undefined)}>
            Cancel
          </button>
        </>
      )}
      {save.isError && (
        <p data-testid="person-form-error" role="alert">
          {save.error.message}
        </p>
      )}
    </form>
  );
}

// The people on the sheet, in the order they were added, and the form that adds or edits them.
function PeoplePanel({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const [editingId, setEditing] = useEditingPerson();
  const editing = people.data?.find((p) => p.id === editingId);

  return (
    <section data-testid="people-panel">
      <h2>People</h2>
      <PersonForm key={editing ? `edit-${editing.id}` : 'add'} sheetId={sheetId} editing={editing} />
      {people.isError && (
        <p data-testid="people-error" role="alert">
          {people.error.message}
        </p>
      )}
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PersonHeads.Slot sheetId={sheetId} user={user} />
            <th />
          </tr>
        </thead>
        <tbody>
          {(people.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <PersonCells.Slot sheetId={sheetId} user={user} personId={p.id} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: PeoplePanel });

import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { GRADES, peopleKey, useEditingPerson, usePeople } from './people';
import type { Person } from './people';

/** One form that adds a person, or edits the one whose editor is open (filled with their values). */
function PersonForm({ sheetId, user, editing }: { sheetId: string; user: string; editing?: Person }) {
  const queryClient = useQueryClient();
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  const [error, setError] = useState('');

  const save = useMutation({
    mutationFn: (body: { user: string; name: string; grade: string }) =>
      editing
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${editing.id}`, body)
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setName('');
      setGrade('A');
      if (editing) setEditing(undefined);
    },
    onError: (e) =>
      setError(
        e instanceof HttpError && e.status === 409 && e.body
          ? e.body
          : 'Could not save this person: a name is required and the grade must be A, B or C.',
      ),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('A name is required.');
      return;
    }
    save.mutate({ user, name, grade });
  };

  return (
    <form data-testid="person-form" onSubmit={submit}>
      <label>
        Name{' '}
        <input
          data-testid="person-form-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
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
            setError('');
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
        <button type="button" data-testid="person-form-cancel" onClick={() => setEditing(undefined)}>
          Cancel
        </button>
      )}
      {error && (
        <p data-testid="person-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's people in the order they were added, each with their name and grade. */
function People({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const [editingId, setEditing] = useEditingPerson();
  const editing = people.data?.find((p) => String(p.id) === editingId);

  return (
    <section data-testid="people">
      <h2>People</h2>
      {/* Keyed by who is being edited, so opening an editor fills the form with their values. */}
      <PersonForm key={editing ? editing.id : 'new'} sheetId={sheetId} user={user} editing={editing} />
      <table>
        <tbody>
          {(people.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(String(p.id))}>
                  Edit
                </button>
              </td>
              <PersonRow.Slot sheetId={sheetId} user={user} person={p} />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: People });

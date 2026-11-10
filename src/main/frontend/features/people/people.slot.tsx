import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { useUser } from '../session/session';
import { GRADES, peopleKey, usePeople } from './people';
import type { Grade, Person } from './people';

type Fields = { name: string; grade: Grade };
const EMPTY: Fields = { name: '', grade: 'A' };

// The people on the sheet: a form that adds a person, or edits the one whose editor is open, and
// the list of everyone in the order they were added.
function People({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [editing, setEditing] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [user] = useUser();

  const reset = () => {
    setFields(EMPTY);
    setEditing(null);
    setError('');
  };

  const save = useMutation({
    mutationFn: ({ id, body }: { id: string | null; body: Fields }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
      return id === null
        ? postJson<Person>(base, body, user)
        : putJson<Person>(`${base}/${encodeURIComponent(id)}`, body, user);
    },
    onSuccess: () => {
      reset();
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: (e) =>
      setError(
        e instanceof HttpError && e.status === 409
          ? 'That name is already in use.'
          : 'That person could not be saved.',
      ),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (fields.name.trim() === '') {
      setError('A name is required.');
      return;
    }
    save.mutate({ id: editing, body: fields });
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  const open = (p: Person) => {
    setEditing(p.id);
    setFields({ name: p.name, grade: p.grade });
    setError('');
  };

  return (
    <section data-testid="people">
      <h2>People</h2>
      <form onSubmit={submit} data-testid="person-form">
        <label>
          Name{' '}
          <input
            data-testid="person-form-name"
            value={fields.name}
            onChange={(e) => change({ name: e.target.value })}
          />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={fields.grade}
            onChange={(e) => change({ grade: e.target.value as Grade })}
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
            <button type="button" data-testid="person-form-cancel" onClick={reset}>
              Cancel
            </button>
          </>
        )}
        {error && (
          <p role="alert" data-testid="person-form-error">
            {error}
          </p>
        )}
      </form>
      {people.isError && (
        <p role="alert" data-testid="people-error">
          The people could not be read.
        </p>
      )}
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th />
            <PersonHead.Slot sheetId={sheetId} />
          </tr>
        </thead>
        <tbody>
          {(people.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                  Edit
                </button>
              </td>
              <PersonRow.Slot sheetId={sheetId} person={p} />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 0, Component: People });

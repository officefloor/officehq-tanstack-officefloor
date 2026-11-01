import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { GRADES, peopleKey, peoplePath, usePeople } from './people';
import type { Grade, Person } from './people';

type Draft = { name: string; grade: Grade };
const EMPTY: Draft = { name: '', grade: 'A' };

// The sheet's people: listed in the order they were added, with one form that adds a person or,
// once a person's editor is opened, edits them.
function People({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);

  const save = useMutation({
    mutationFn: ({ id, body }: { id: number | null; body: Draft }) =>
      id === null
        ? postJson<Person[]>(byUser(peoplePath(sheet), user), body)
        : putJson<Person[]>(byUser(`${peoplePath(sheet)}/${id}`, user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
      setDraft(EMPTY);
      setEditing(null);
    },
  });

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const open = (p: Person) => {
    setEditing(p.id);
    setDraft({ name: p.name, grade: p.grade });
    save.reset();
  };

  const cancel = () => {
    setEditing(null);
    setDraft(EMPTY);
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.reset();
    // The server refuses an empty name or one already on the sheet; its refusal shows as the form's error.
    save.mutate({ id: editing, body: draft });
  };

  return (
    <section>
      <h2>People</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input data-testid="person-form-name" value={draft.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={draft.grade}
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
          {editing === null ? 'Add person' : 'Save person'}
        </button>
        {editing !== null && (
          <button type="button" data-testid="person-form-cancel" onClick={cancel}>
            Cancel
          </button>
        )}
        {save.isError && (
          <span data-testid="person-form-error" role="alert">
            {' '}A person needs a name not already on the sheet and a grade of A, B or C.
          </span>
        )}
      </form>
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PersonColumnHeaders.Slot sheet={sheet} user={user} />
            <th />
          </tr>
        </thead>
        <tbody>
          {people.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <PersonColumns.Slot sheet={sheet} user={user} person={p} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
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

export const contribution = SheetPanels.fill({ order: 10, Component: People });

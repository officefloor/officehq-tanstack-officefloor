import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useCurrentSheet } from '../session/session';
import { GRADES, addPerson, peopleKey, updatePerson, useEditingPerson, usePeople } from './people';
import type { Person, PersonInput } from './people';

const EMPTY: PersonInput = { name: '', grade: 'A' };

// The sheet's people: a form that adds a person or, once a person's editor is opened, edits them;
// and the list of people in the order they were added.
function People() {
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const [editing, setEditing] = useEditingPerson();
  const [form, setForm] = useState<PersonInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (input: PersonInput) =>
      editing ? updatePerson(sheet!, Number(editing), input) : addPerson(sheet!, input),
    onSuccess: async () => {
      setForm(EMPTY);
      setEditing(undefined);
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
    onError: () => setError('The person could not be saved: a name is required and must not already be in use.'),
  });

  if (!sheet) return null;

  const change = (patch: Partial<PersonInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const open = (person: Person) => {
    setEditing(String(person.id));
    setForm({ name: person.name, grade: person.grade });
    setError('');
  };

  const cancel = () => {
    setEditing(undefined);
    setForm(EMPTY);
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.name.trim()) {
      setError('A person needs a name.');
      return;
    }
    save.mutate(form);
  };

  return (
    <section data-testid="people">
      <h2>People</h2>
      <form data-testid="person-form" onSubmit={submit}>
        <input
          data-testid="person-form-name"
          placeholder="Name"
          value={form.name}
          onChange={(e) => change({ name: e.target.value })}
        />
        <select
          data-testid="person-form-grade"
          value={form.grade}
          onChange={(e) => change({ grade: e.target.value })}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing ? 'Save' : 'Add person'}
        </button>
        {editing && (
          <button type="button" data-testid="person-form-cancel" onClick={cancel}>
            Cancel
          </button>
        )}
        {error && <span data-testid="person-form-error">{error}</span>}
      </form>
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PersonColumns.Slot />
            <th />
          </tr>
        </thead>
        <tbody>
          {(people.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <PersonCells.Slot personId={p.id} />
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

export const contribution = HomeMain.fill({ order: 10, Component: People });

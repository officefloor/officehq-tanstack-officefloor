import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES, peopleKey, useEditingPerson, usePeople } from './people';
import type { Person, PersonInput } from './people';

const EMPTY: PersonInput = { name: '', grade: 'A' };

// Adds a person, or — while a person's editor is open — saves that person's changes. Opening an
// editor fills the form with the person's values.
export function PersonForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [editing, setEditing] = useEditingPerson();
  const people = usePeople();
  const [form, setForm] = useState<PersonInput>(EMPTY);
  const [error, setError] = useState('');
  const filledFor = useRef<number | undefined>(undefined);
  const queryClient = useQueryClient();

  // Fill the form once per opened editor; close an editor whose person is not on this sheet.
  useEffect(() => {
    if (editing === undefined) {
      filledFor.current = undefined;
      return;
    }
    if (!people.data || filledFor.current === editing) return;
    const person = people.data.find((p) => p.id === editing);
    if (!person) {
      setEditing(undefined);
      return;
    }
    filledFor.current = editing;
    setForm({ name: person.name, grade: person.grade });
    setError('');
  }, [editing, people.data, setEditing]);

  const close = () => {
    setEditing(undefined);
    setForm(EMPTY);
    setError('');
  };

  const save = useMutation({
    mutationFn: (input: PersonInput) =>
      editing === undefined
        ? postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people`, input)
        : putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${editing}`, input),
    onSuccess: async () => {
      close();
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: () => setError('That person could not be saved.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.name.trim()) {
      setError('Type a name.');
      return;
    }
    save.mutate(form);
  };

  const change = (patch: Partial<PersonInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <form onSubmit={submit} data-testid="person-form">
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
        {editing === undefined ? 'Add person' : 'Save person'}
      </button>
      {editing !== undefined && (
        <button type="button" data-testid="person-form-cancel" onClick={close}>
          Cancel
        </button>
      )}
      {error && <span data-testid="person-form-error">{error}</span>}
    </form>
  );
}

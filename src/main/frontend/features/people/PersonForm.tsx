import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES, peopleKey, useEditingPerson, usePeople } from './people';
import type { Person, PersonInput } from './people';

const EMPTY: PersonInput = { name: '', grade: 'A', gradeFrom: '', terms: '' };

// Adds a person, or — while a person's editor is open — saves that person's changes. Opening an
// editor fills the form with the person's values and remembers the version it loaded: a save over
// someone else's later save is refused, and the form then shows the current values to save again.
export function PersonForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [editing, setEditing] = useEditingPerson();
  const people = usePeople();
  const [form, setForm] = useState<PersonInput>(EMPTY);
  const [error, setError] = useState('');
  const filledFor = useRef<number | undefined>(undefined);
  const version = useRef<number | undefined>(undefined);
  const once = useOneAtATime();
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
    version.current = person.version;
    setForm({ name: person.name, grade: person.grade, gradeFrom: '', terms: person.terms == null ? '' : String(person.terms) });
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
        ? postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people?user=${encodeURIComponent(user)}`, input)
        : putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${editing}?user=${encodeURIComponent(user)}`, {
            ...input,
            version: version.current,
          }),
    onSuccess: async () => {
      close();
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: async (e, input) => {
      if (e instanceof HttpError && e.status === 409) {
        // Someone else saved this person since the editor loaded it: show what is stored now.
        await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
        const current = queryClient.getQueryData<Person[]>(peopleKey(sheet))?.find((p) => p.id === editing);
        if (current) {
          version.current = current.version;
          setForm({ name: current.name, grade: current.grade, gradeFrom: '', terms: current.terms == null ? '' : String(current.terms) });
        }
        setError('Someone else saved this person meanwhile; the form now shows their values.');
        return;
      }
      const key = input.name.trim().toLowerCase();
      const taken = people.data?.some((p) => p.id !== editing && p.name.trim().toLowerCase() === key);
      // A refused edit changes nothing: the editor closes, back to adding, with the refusal showing.
      if (editing !== undefined) {
        setEditing(undefined);
        setForm(EMPTY);
      }
      setError(taken ? 'That name is already in use.' : 'That person could not be saved.');
    },
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.name.trim()) {
      setError('Type a name.');
      return;
    }
    once(() => save.mutateAsync(form));
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
      <input
        data-testid="person-form-grade-from"
        type="date"
        title="Grade from (blank: the sheet's today)"
        value={form.gradeFrom}
        onChange={(e) => change({ gradeFrom: e.target.value })}
      />
      <input
        data-testid="person-form-terms"
        placeholder="Terms (days)"
        title="The person's own terms in days (blank: none)"
        value={form.terms}
        onChange={(e) => change({ terms: e.target.value })}
      />
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

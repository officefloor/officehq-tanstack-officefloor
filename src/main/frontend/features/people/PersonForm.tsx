import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { GRADES, peopleKey, peopleQuery } from './peopleQuery';
import type { Grade, Person } from './peopleQuery';
import { useEditingPerson } from './useEditingPerson';

/** The one form people are added and edited through. Opening a person's editor fills it. */
export function PersonForm({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const [editingId, setEditingId] = useEditingPerson();
  const editing = people.find((p) => p.id === editingId);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<Grade>('A');
  const [error, setError] = useState('');

  // Opening an editor (or closing it) refills the form.
  useEffect(() => {
    setName(editing?.name ?? '');
    setGrade(editing?.grade ?? 'A');
    setError('');
  }, [editing?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  const save = useMutation({
    mutationFn: (body: { name: string; grade: Grade }) =>
      editing
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${editing.id}`, body)
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people`, body),
    onSuccess: () => {
      if (editing) {
        setEditingId(undefined);
      } else {
        setName('');
        setGrade('A');
      }
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('The person could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('Type a name.');
      return;
    }
    save.mutate({ name, grade });
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
            setGrade(e.target.value as Grade);
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
        {editing ? 'Save person' : 'Add person'}
      </button>
      {editing && (
        <button type="button" data-testid="person-form-cancel" onClick={() => setEditingId(undefined)}>
          Cancel
        </button>
      )}
      {error && <p data-testid="person-form-error">{error}</p>}
    </form>
  );
}

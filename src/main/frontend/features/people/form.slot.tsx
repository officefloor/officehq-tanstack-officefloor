import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { GRADES, type Person, peopleKey, useEditingPerson, usePeople } from './people';

// Adds a person, or edits the one whose editor is open (opening it fills the form with their values).
function PersonForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [editing, setEditing] = useEditingPerson();
  const people = usePeople(sheet);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheet)}/people`;

  const save = useMutation({
    mutationFn: (p: { name: string; grade: string }) =>
      editing
        ? putJson<Person>(`${base}/${encodeURIComponent(editing)}`, { ...p, user })
        : postJson<Person>(base, { ...p, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      setName('');
      setGrade('A');
      setEditing(undefined);
    },
  });

  const editingPerson = people.data?.find((p) => String(p.id) === editing);
  useEffect(() => {
    save.reset();
    if (editingPerson) {
      setName(editingPerson.name);
      setGrade(editingPerson.grade);
    }
    // Fill only when a different editor opens, not on every re-read of the list.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editing, editingPerson?.id]);

  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        save.mutate({ name, grade });
      }}
    >
      <h2>{editing ? 'Edit person' : 'Add person'}</h2>
      <input
        data-testid="person-form-name"
        placeholder="Name"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          save.reset();
        }}
      />
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
      <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
        {editing ? 'Save' : 'Add'}
      </button>
      {editing && (
        <button
          type="button"
          data-testid="person-form-cancel"
          onClick={() => {
            setEditing(undefined);
            setName('');
            setGrade('A');
          }}
        >
          Cancel
        </button>
      )}
      {save.isError && (
        <span data-testid="person-form-error" role="alert">
          {' '}A name is required.
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 10, Component: PersonForm });

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES, peoplePath, peopleQueryKey, usePeople, useEditingPerson, type Person } from './people';

// The person form: adds a person, or — while a person's editor is open — saves their changes.
function PersonForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [editing] = useEditingPerson();
  const people = usePeople();
  if (!user || !sheet) return null;
  const person = editing ? people.data?.find((p) => String(p.id) === editing) : undefined;
  // Keyed by who is being edited, so opening an editor fills the form with that person's values;
  // an editor for someone not on this sheet falls back to the add form.
  return <PersonFormFields key={`${sheet}:${person?.id ?? ''}`} sheet={sheet} person={person} />;
}

function PersonFormFields({ sheet, person }: { sheet: string; person?: Person }) {
  const [user] = useCurrentUser();
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(person?.name ?? '');
  const [grade, setGrade] = useState(person?.grade ?? 'A');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { name: string; grade: string }) =>
      person
        ? putJson<Person>(`${peoplePath(sheet)}/${person.id}`, body, user)
        : postJson<Person>(peoplePath(sheet), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) });
      if (person) {
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
        save.reset();
        save.mutate({ name, grade });
      }}
    >
      <h2>{person ? 'Edit person' : 'Add a person'}</h2>
      <input
        data-testid="person-form-name"
        placeholder="Name"
        value={name}
        onChange={(e) => { setName(e.target.value); save.reset(); }}
      />
      <select
        data-testid="person-form-grade"
        value={grade}
        onChange={(e) => { setGrade(e.target.value); save.reset(); }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>
      <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
        {person ? 'Save' : 'Add person'}
      </button>
      {person && (
        <button type="button" data-testid="person-form-cancel" onClick={() => setEditing(undefined)}>
          Cancel
        </button>
      )}
      {save.isError && <span data-testid="person-form-error">A name is required and must not already be in use on this sheet, and the grade must be A, B or C.</span>}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 10, Component: PersonForm });

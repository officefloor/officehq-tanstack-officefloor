import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES, peoplePath, peopleQueryKey, usePeople, useEditingPerson, type Person } from './people';

// The person form: adds a person, or — while a person's editor is open — saves their changes. The
// editor remembers the version of the person it loaded; when someone else has saved them since, the
// save is refused and the form shows the person's current values (and version) instead.
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
  const [version, setVersion] = useState(person?.version);
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { name: string; grade: string }) =>
      person
        ? putJson<Person>(`${peoplePath(sheet)}/${person.id}`, { ...body, version }, user)
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
    onError: async (error) => {
      if (!person || !(error instanceof HttpError) || error.status !== 409) return;
      const current = await queryClient.fetchQuery({
        queryKey: peopleQueryKey(sheet),
        queryFn: () => getJson<Person[]>(peoplePath(sheet)),
        staleTime: 0,
      });
      const now = current.find((p) => p.id === person.id);
      if (now) {
        setName(now.name);
        setGrade(now.grade);
        setVersion(now.version);
      }
    },
  });
  const submit = useSubmitOnce(save);
  const conflict = save.error instanceof HttpError && save.error.status === 409;

  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ name, grade });
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
      {save.isError && (
        <span data-testid="person-form-error">
          {conflict
            ? 'Someone else has saved this person since you opened the editor; the form now shows their current values.'
            : 'A name is required and must not already be in use on this sheet, and the grade must be A, B or C.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 10, Component: PersonForm });

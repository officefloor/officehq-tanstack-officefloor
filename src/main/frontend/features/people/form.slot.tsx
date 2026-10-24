import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES, personSaveKey, peoplePath, peopleQueryKey, usePeople, useEditingPerson, type Person } from './people';

// The person form: adds a person, or — while a person's editor is open — saves their changes. A save records the
// grade from the from-date (blank: the sheet's today), keeping the grades already recorded; the grade a person is
// added with applies from the beginning of time. A person's own terms (blank: none) come ahead of their grade's
// and the sheet's when a claim's due-by is fixed. The
// editor remembers the version of the person it loaded; when someone else has saved them since, the
// save is refused and the form shows the person's current values (and version) instead. Any other refused save
// closes the editor, its error staying on the form until the form is next changed or submitted, an editor is
// opened or another sheet is loaded.
function PersonForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [editing] = useEditingPerson();
  const people = usePeople();
  const [refused, setRefused] = useState(false);
  useEffect(() => {
    if (editing) setRefused(false);
  }, [editing]);
  useEffect(() => setRefused(false), [sheet]);
  if (!user || !sheet) return null;
  const person = editing ? people.data?.find((p) => String(p.id) === editing) : undefined;
  // Keyed by who is being edited, so opening an editor fills the form with that person's values;
  // an editor for someone not on this sheet falls back to the add form.
  return <PersonFormFields key={`${sheet}:${person?.id ?? ''}`} sheet={sheet} person={person}
    refused={refused} setRefused={setRefused} />;
}

function PersonFormFields({ sheet, person, refused, setRefused }: {
  sheet: string;
  person?: Person;
  refused: boolean;
  setRefused: (refused: boolean) => void;
}) {
  const [user] = useCurrentUser();
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(person?.name ?? '');
  const [grade, setGrade] = useState(person?.grade ?? 'A');
  const [gradeFrom, setGradeFrom] = useState('');
  const [terms, setTerms] = useState(person?.terms == null ? '' : String(person.terms));
  const [version, setVersion] = useState(person?.version);
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationKey: personSaveKey,
    mutationFn: (body: { name: string; grade: string; gradeFrom: string; terms: string }) =>
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
        setGradeFrom('');
        setTerms('');
      }
    },
    onError: async (error) => {
      if (!person) return;
      if (!(error instanceof HttpError) || error.status !== 409) {
        setRefused(true);
        setEditing(undefined);
        return;
      }
      const current = await queryClient.fetchQuery({
        queryKey: peopleQueryKey(sheet),
        queryFn: () => getJson<Person[]>(peoplePath(sheet)),
        staleTime: 0,
      });
      const now = current.find((p) => p.id === person.id);
      if (now) {
        setName(now.name);
        setGrade(now.grade);
        setTerms(now.terms == null ? '' : String(now.terms));
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
        setRefused(false);
        submit({ name, grade, gradeFrom: gradeFrom.trim(), terms: terms.trim() });
      }}
    >
      <h2>{person ? 'Edit person' : 'Add a person'}</h2>
      <input
        data-testid="person-form-name"
        placeholder="Name"
        value={name}
        onChange={(e) => { setName(e.target.value); save.reset(); setRefused(false); }}
      />
      <select
        data-testid="person-form-grade"
        value={grade}
        onChange={(e) => { setGrade(e.target.value); save.reset(); setRefused(false); }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>
      <input
        data-testid="person-form-grade-from"
        placeholder="Grade from (YYYY-MM-DD)"
        value={gradeFrom}
        onChange={(e) => { setGradeFrom(e.target.value); save.reset(); setRefused(false); }}
      />
      <input
        data-testid="person-form-terms"
        placeholder="Terms (days; blank for none)"
        value={terms}
        onChange={(e) => { setTerms(e.target.value); save.reset(); setRefused(false); }}
      />
      <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
        {person ? 'Save' : 'Add person'}
      </button>
      {person && (
        <button type="button" data-testid="person-form-cancel" onClick={() => setEditing(undefined)}>
          Cancel
        </button>
      )}
      {(save.isError || refused) && (
        <span data-testid="person-form-error">
          {conflict
            ? 'Someone else has saved this person since you opened the editor; the form now shows their current values.'
            : 'A name is required and must not already be in use on this sheet, the grade must be A, B or C, the grade from-date must be a date, and the terms must be a whole number of days or blank.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 10, Component: PersonForm });

import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { GRADES, type Person, peopleKey, useEditingPerson, usePeople } from './people';

// Adds a person, or edits the one whose editor is open (opening it fills the form with their values
// and remembers the version it loaded). A save over a version someone else has since saved is
// refused; the form then shows the person's current values and version, so saving again works.
// An edit records the grade from the from-date (the sheet's today when blank), keeping the grades
// already recorded; leaving the grade as the row shows it with the from-date blank records nothing.
// An added person's grade applies from the beginning of time. Their own terms (whole days, blank for
// none) come first when a claim's due-by is fixed. A refused edit closes the editor, keeping its error,
// so the next add is not taken for an edit.
function PersonForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [editing, setEditing] = useEditingPerson();
  const people = usePeople(sheet);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const [gradeFrom, setGradeFrom] = useState('');
  const [terms, setTerms] = useState('');
  const keepError = useRef(false);
  const [version, setVersion] = useState<number | undefined>();
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheet)}/people`;

  const save = useMutation({
    mutationFn: (p: { name: string; grade: string; gradeFrom: string; terms: string }) =>
      editing
        ? putJson<Person>(`${base}/${encodeURIComponent(editing)}`, { ...p, user, version })
        : postJson<Person>(base, { ...p, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      setName('');
      setGrade('A');
      clear();
    },
    onError: async (error) => {
      if (!(error instanceof HttpError && error.status === 409)) {
        if (editing) {
          keepError.current = true;
          clear();
        }
        return;
      }
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      const current = queryClient.getQueryData<Person[]>(peopleKey(sheet))?.find((p) => String(p.id) === editing);
      if (current) fill(current);
    },
  });
  const submit = useSubmitOnce(save);
  const fill = (p: Person) => {
    setName(p.name);
    setGrade(p.grade);
    setGradeFrom('');
    setTerms(p.terms === null ? '' : String(p.terms));
    setVersion(p.version);
  };
  const clear = () => {
    setName('');
    setGrade('A');
    setGradeFrom('');
    setTerms('');
    setVersion(undefined);
    setEditing(undefined);
  };

  const editingPerson = people.data?.find((p) => String(p.id) === editing);
  useEffect(() => {
    if (keepError.current) keepError.current = false;
    else save.reset();
    if (editingPerson) fill(editingPerson);
    // Fill only when a different editor opens, not on every re-read of the list.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editing, editingPerson?.id]);

  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ name, grade, gradeFrom, terms });
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
      <input
        data-testid="person-form-grade-from"
        type="date"
        aria-label="Grade from (blank for the sheet's today)"
        value={gradeFrom}
        onChange={(e) => {
          setGradeFrom(e.target.value);
          save.reset();
        }}
      />
      <input
        data-testid="person-form-terms"
        inputMode="numeric"
        placeholder="Terms (days)"
        aria-label="Own terms in days (blank for none)"
        value={terms}
        onChange={(e) => {
          setTerms(e.target.value);
          save.reset();
        }}
      />
      <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
        {editing ? 'Save' : 'Add'}
      </button>
      {editing && (
        <button
          type="button"
          data-testid="person-form-cancel"
          onClick={() => {
            save.reset();
            clear();
          }}
        >
          Cancel
        </button>
      )}
      {save.isError && (
        <span data-testid="person-form-error" role="alert">
          {' '}
          {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'A name is required.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 10, Component: PersonForm });

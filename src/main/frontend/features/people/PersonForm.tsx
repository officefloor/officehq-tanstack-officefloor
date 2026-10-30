import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSheetToday } from '../sheet/useSheetToday';
import { tagList, tagsKey } from '../claims/useClaims';
import { useEditorOpens } from './editorOpens';
import { GRADES, gradeOn, peopleKey, usePeople, type Person } from './usePeople';

// Adds a person, or — while a person's editor is open (`editPerson` in the URL) — saves that
// person. Opening the editor fills the form with the person's values and remembers the version it
// loaded; a save over someone else's newer save is refused and the form shows the current values.
// The grade is dated: a save records the form's grade from its from-date (the sheet's today when
// blank), except that leaving the grade as the row showed it with a blank from-date records nothing.
// A person's first grade applies from the beginning of time, so an add takes no from-date.
// A person's own terms (whole days, blank for none) come first in fixing a new claim's due-by: those
// in force on the day the claim is created (the sheet's today). Terms with a from-date are recorded from it, keeping those already
// recorded; with a blank from-date they are the terms from the beginning of time, which the editor
// opens with (the row shows those in force on the sheet's today).
// A contact (blank for none) is stored trimmed and lower-cased; one another person on the sheet
// already has is refused.
// Tags are comma separated: each is trimmed and lower-cased, and repeats are dropped (by the server).
// Terms that are not a whole number are refused here, before anything is sent: the error shows and
// the editor (if one was open) closes, so the form is back to adding.
export function PersonForm() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editParam, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
  const edited = people.find((p) => p.id === editParam);
  // A person not on this sheet (an id left from another sheet) has no editor open.
  const editing = edited?.id ?? '';
  // The editor open now, read when a save answers: another opened meanwhile is left as it is.
  const editingNow = useRef(editing);
  editingNow.current = editing;
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const [gradeFrom, setGradeFrom] = useState('');
  const [terms, setTerms] = useState('');
  const [termsFrom, setTermsFrom] = useState('');
  const [contact, setContact] = useState('');
  const [tags, setTags] = useState('');
  const [termsRefused, setTermsRefused] = useState(false);
  const today = useSheetToday(sheet);
  const [version, setVersion] = useState<number | undefined>(undefined);
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const save = useMutation({
    mutationFn: ({ editing, ...body }: { editing: string; name: string; grade: string; gradeFrom: string; terms: string; termsFrom: string; contact: string; tags: string }) =>
      editing !== ''
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(editing)}`, { ...body, user, version })
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people`, { ...body, user }),
    onSuccess: (_saved, { editing }) => {
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: tagsKey(sheet) });
      if (editing !== editingNow.current) return;
      setName('');
      setGrade('A');
      setGradeFrom('');
      setTerms('');
      setTermsFrom('');
      setContact('');
      setTags('');
      if (editing !== '') setEditing(undefined);
    },
    onError: async (error) => {
      if (!(error instanceof HttpError) || error.status !== 409 || editing === '') return;
      // Someone else saved this person since the editor loaded it: show what is stored now.
      const fresh = await queryClient.fetchQuery({
        queryKey: peopleKey(sheet),
        queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
        staleTime: 0,
      });
      const current = fresh.find((p) => p.id === editing);
      if (current) {
        setName(current.name);
        setGrade(gradeOn(current, today));
        setTerms(current.baseTerms);
        setTermsFrom('');
        setContact(current.contact);
        setTags(tagList(current.tags));
        setVersion(current.version);
      }
    },
  });

  const clearError = () => {
    save.reset();
    setTermsRefused(false);
  };

  // Opening an editor (pressing edit again too) fills the form with that person's values.
  const editedId = edited?.id;
  const opens = useEditorOpens();
  useEffect(() => {
    save.reset();
    if (edited) {
      setTermsRefused(false);
      setName(edited.name);
      setGrade(gradeOn(edited, today));
      setGradeFrom('');
      setTerms(edited.baseTerms);
      setTermsFrom('');
      setContact(edited.contact);
      setTags(tagList(edited.tags));
      setVersion(edited.version);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editedId, opens]);

  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        clearError();
        if (!/^\s*\d*\s*$/.test(terms)) {
          setTermsRefused(true);
          if (editing !== '') {
            setEditing(undefined);
            setName('');
            setGrade('A');
            setGradeFrom('');
            setTerms('');
            setTermsFrom('');
            setContact('');
            setTags('');
          }
          return;
        }
        submitOnce(() => {
          clearError();
          return save.mutateAsync({ editing, name, grade, gradeFrom, terms, termsFrom, contact, tags });
        });
      }}
    >
      <input
        data-testid="person-form-name"
        value={name}
        placeholder="Name"
        onChange={(e) => {
          setName(e.target.value);
          clearError();
        }}
      />{' '}
      <select
        data-testid="person-form-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          clearError();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <input
        type="date"
        data-testid="person-form-grade-from"
        title="Grade from (blank: the sheet's today)"
        value={gradeFrom}
        onChange={(e) => {
          setGradeFrom(e.target.value);
          clearError();
        }}
      />{' '}
      <input
        data-testid="person-form-terms"
        value={terms}
        placeholder="Terms (days)"
        inputMode="numeric"
        onChange={(e) => {
          setTerms(e.target.value);
          clearError();
        }}
      />{' '}
      <input
        type="date"
        data-testid="person-form-terms-from"
        title="Terms from (blank: the beginning of time)"
        value={termsFrom}
        onChange={(e) => {
          setTermsFrom(e.target.value);
          clearError();
        }}
      />{' '}
      <input
        data-testid="person-form-contact"
        value={contact}
        placeholder="Contact"
        onChange={(e) => {
          setContact(e.target.value);
          clearError();
        }}
      />{' '}
      <input
        data-testid="person-form-tags"
        value={tags}
        placeholder="Tags"
        onChange={(e) => {
          setTags(e.target.value);
          clearError();
        }}
      />{' '}
      <button type="submit" data-testid="person-form-submit">{editing !== '' ? 'Save' : 'Add person'}</button>
      {editing !== '' && (
        <button
          type="button"
          data-testid="person-form-cancel"
          onClick={() => {
            setEditing(undefined);
            setName('');
            setGrade('A');
            setGradeFrom('');
            setTerms('');
            setTermsFrom('');
            setContact('');
            setTags('');
            clearError();
          }}
        >
          Cancel
        </button>
      )}
      {(save.isError || termsRefused) && (
        <span data-testid="person-form-error" role="alert">
          {termsRefused
            ? ' Not saved: terms must be a whole number of days, or blank for none.'
            : save.error instanceof HttpError && save.error.status === 409
            ? ' Not saved: someone else changed this person since you opened it. The form now shows their values.'
            : ' Could not save: a name is required and must not already be in use, a contact must not already be in use, a from-date must be a date, and terms must be a whole number of days.'}
        </span>
      )}
    </form>
  );
}

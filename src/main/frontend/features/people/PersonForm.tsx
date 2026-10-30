import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ConflictError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { tagsKey } from '../claims/claimsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useUser } from '../session/useUser';
import { peopleKey, peopleQuery } from './peopleQuery';
import { useGrades } from '../grades/gradesQuery';
import type { Grade, Person } from './peopleQuery';
import { useEditingPerson } from './useEditingPerson';

/** The one form people are added and edited through. Opening a person's editor fills it. */
export function PersonForm({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const sheetGrades = useGrades(sheetId);
  const [editingId, setEditingId] = useEditingPerson();
  const editing = people.find((p) => p.id === editingId);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<Grade>('A');
  // The date an edit's grade is given from (yyyy-mm-dd); blank for the sheet's today.
  const [gradeFrom, setGradeFrom] = useState('');
  // The person's own terms in days; blank for none.
  const [terms, setTerms] = useState('');
  // The date an edit's terms are recorded from (yyyy-mm-dd); blank for the beginning of time.
  const [termsFrom, setTermsFrom] = useState('');
  // The person's contact; blank for none.
  const [contact, setContact] = useState('');
  // The person's tags, comma separated; blank for none.
  const [tags, setTags] = useState('');
  const today = useSheetToday(sheetId);
  const [error, setError] = useState('');
  // The version of the person the editor loaded; a save is refused once someone else has saved since.
  const [version, setVersion] = useState<number | undefined>(undefined);

  const fill = (p: Person | undefined) => {
    setName(p?.name ?? '');
    setGrade(p?.grade ?? 'A');
    setGradeFrom('');
    setTerms(p?.terms == null ? '' : String(p.terms));
    setTermsFrom('');
    setContact(p?.contact ?? '');
    setTags(p?.tags.join(', ') ?? '');
    setVersion(p?.version);
  };

  // Set when a refused save closes the editor, so its error stays showing.
  const keepError = useRef(false);

  // Opening an editor (or closing it) refills the form.
  useEffect(() => {
    fill(editing);
    if (!keepError.current) setError('');
    keepError.current = false;
  }, [editing?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: { name: string; grade: Grade; version?: number; gradeFrom?: string; terms: string; termsFrom?: string; contact: string; tags: string }) =>
      editing
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${editing.id}`, body, user)
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people`, body, user),
    onSuccess: () => {
      if (editing) {
        setEditingId(undefined);
      } else {
        setName('');
        setGrade('A');
        setGradeFrom('');
        setTerms('');
        setTermsFrom('');
        setContact('');
        setTags('');
      }
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      // A person's new tags join the tags used on the sheet.
      void queryClient.invalidateQueries({ queryKey: tagsKey(sheetId) });
    },
    onError: async (e) => {
      if (e instanceof ConflictError && editing) {
        // Someone else saved this person since the editor loaded it: show what is stored now.
        setError('Someone else has saved this person since you opened it. The form now shows their changes.');
        const fresh = await queryClient.fetchQuery({ ...peopleQuery(sheetId), staleTime: 0 });
        fill(fresh.find((p) => p.id === editing.id));
      } else {
        setError('The person could not be saved.');
        if (editing) {
          // A refused edit closes the editor, storing nothing, so the form is back to adding a person.
          keepError.current = true;
          setEditingId(undefined);
        }
      }
    },
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const trimmed = name.trim();
    if (trimmed === '') {
      setError('Type a name.');
      return;
    }
    // Names are unique on the sheet, ignoring case; the server refuses one it finds in use too.
    if (people.some((p) => p.id !== editing?.id && p.name.toLowerCase() === trimmed.toLowerCase())) {
      setError('That name is already in use.');
      return;
    }
    // Contacts are unique on the sheet; the server refuses one in use (after its version check, so a stale editor is refreshed).
    once(() => save.mutateAsync(editing ? { name: trimmed, grade, version, gradeFrom: gradeFrom.trim(), terms: terms.trim(), termsFrom: termsFrom.trim(), contact: contact.trim(), tags } : { name: trimmed, grade, terms: terms.trim(), contact: contact.trim(), tags }));
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
          {sheetGrades.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        Grade from{' '}
        <input
          type="date"
          data-testid="person-form-grade-from"
          value={gradeFrom}
          placeholder={today}
          onChange={(e) => {
            setGradeFrom(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Terms (days){' '}
        <input
          data-testid="person-form-terms"
          value={terms}
          inputMode="numeric"
          onChange={(e) => {
            setTerms(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Terms from{' '}
        <input
          type="date"
          data-testid="person-form-terms-from"
          value={termsFrom}
          onChange={(e) => {
            setTermsFrom(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Contact{' '}
        <input
          data-testid="person-form-contact"
          value={contact}
          onChange={(e) => {
            setContact(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Tags{' '}
        <input
          data-testid="person-form-tags"
          value={tags}
          onChange={(e) => {
            setTags(e.target.value);
            setError('');
          }}
        />
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

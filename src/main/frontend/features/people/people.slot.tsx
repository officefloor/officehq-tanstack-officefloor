import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { HttpError, postJson, putJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { useUser } from '../session/session';
import { nameWithGrade, peopleKey, peopleQuery, usePeople, useShowRetired } from './people';
import type { Grade, Person } from './people';
import { SORT_KEYS, useSorted, useSortBy } from './sort';
import type { SortKey } from './sort';
import { Col } from '../columns/columns';
import { useVisiblePeople } from './visible';
import { useGradeCodes } from '../grades/grades';

// `gradeFrom` is the date the grade is from ('' for the sheet's today). A person's first grade
// applies from the beginning of time; an edit records the grade from its date, keeping the others,
// but records nothing when it leaves the grade as the row shows it with no from-date. `terms` are
// their own terms in days ('' for none); `termsFrom` the date they are from ('' for the beginning of
// time): dated terms are kept alongside those already recorded. `contact` is their contact ('' for none). `tags` are their tags, comma separated ('' for none).
type Fields = { name: string; grade: Grade; gradeFrom: string; terms: string; termsFrom: string; contact: string; tags: string };
const EMPTY: Fields = { name: '', grade: 'A', gradeFrom: '', terms: '', termsFrom: '', contact: '', tags: '' };

// The people on the sheet: a form that adds a person, or edits the one whose editor is open, and
// the list of everyone in list order (new people at the end; up and down buttons move them) (retired people only when "show retired" is on). The editor remembers the version of the person
// it loaded: if someone else has saved them since, the save is refused and the editor shows the
// person as stored now (and their version, so saving again goes through).
function People({ sheetId }: { sheetId: string }) {
  const GRADES = useGradeCodes(sheetId);
  const queryClient = useQueryClient();
  const people = usePeople(sheetId);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [editing, setEditing] = useState<string | null>(null);
  const [version, setVersion] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [user] = useUser();
  const [showRetired, setShowRetired] = useShowRetired();
  const [sortBy, setSortBy] = useSortBy();
  const visible = useVisiblePeople(sheetId, user);
  const shown = useSorted(
    sheetId,
    (people.data ?? []).filter((p) => (showRetired || !p.retired) && visible(p)),
  );

  const reset = () => {
    setFields(EMPTY);
    setEditing(null);
    setVersion(null);
    setError('');
  };

  const once = useOnce();
  const save = useMutation({
    mutationFn: ({ id, body }: { id: string | null; body: Fields & { version?: number } }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
      return id === null
        ? postJson<Person>(base, body, user)
        : putJson<Person>(`${base}/${encodeURIComponent(id)}`, body, user);
    },
    onSuccess: () => {
      reset();
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: async (e, { id }) => {
      if (e instanceof HttpError && e.status === 412 && id !== null) {
        setError('Someone else has changed this person. Their values are shown; save again to change them.');
        const current = (await queryClient.fetchQuery({ ...peopleQuery(sheetId), staleTime: 0 })).find(
          (p) => p.id === id,
        );
        if (current) {
          setFields({ name: current.name, grade: current.grade, gradeFrom: '', terms: current.terms, termsFrom: '', contact: current.contact, tags: current.tags.join(', ') });
          setVersion(current.version);
        }
        return;
      }
      // Any other refused edit closes the editor, so the form is back to adding; the error stays.
      if (id !== null) {
        setFields(EMPTY);
        setEditing(null);
        setVersion(null);
      }
      setError(
        e instanceof HttpError && e.status === 409
          ? 'That name or contact is already in use.'
          : 'That person could not be saved.',
      );
    },
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (fields.name.trim() === '') {
      setError('A name is required.');
      return;
    }
    once((done) => save.mutate({ id: editing, body: version === null ? fields : { ...fields, version } }, { onSettled: done }));
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  const open = (p: Person) => {
    setEditing(p.id);
    setFields({ name: p.name, grade: p.grade, gradeFrom: '', terms: p.terms, termsFrom: '', contact: p.contact, tags: p.tags.join(', ') });
    setVersion(p.version);
    setError('');
  };

  return (
    <section data-testid="people">
      <h2>People</h2>
      <form onSubmit={submit} data-testid="person-form">
        <label>
          Name{' '}
          <input
            data-testid="person-form-name"
            value={fields.name}
            onChange={(e) => change({ name: e.target.value })}
          />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={fields.grade}
            onChange={(e) => change({ grade: e.target.value as Grade })}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="person-form-grade-from"
            value={fields.gradeFrom}
            onChange={(e) => change({ gradeFrom: e.target.value })}
          />
        </label>{' '}
        <label>
          Terms{' '}
          <input
            data-testid="person-form-terms"
            inputMode="numeric"
            size={4}
            value={fields.terms}
            onChange={(e) => change({ terms: e.target.value })}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="person-form-terms-from"
            value={fields.termsFrom}
            onChange={(e) => change({ termsFrom: e.target.value })}
          />
        </label>{' '}
        <label>
          Contact{' '}
          <input
            data-testid="person-form-contact"
            value={fields.contact}
            onChange={(e) => change({ contact: e.target.value })}
          />
        </label>{' '}
        <label>
          Tags{' '}
          <input
            data-testid="person-form-tags"
            placeholder="Tags, comma separated"
            value={fields.tags}
            onChange={(e) => change({ tags: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add' : 'Save'}
        </button>
        {editing !== null && (
          <>
            {' '}
            <button type="button" data-testid="person-form-cancel" onClick={reset}>
              Cancel
            </button>
          </>
        )}
        {error && (
          <p role="alert" data-testid="person-form-error">
            {error}
          </p>
        )}
      </form>
      {people.isError && (
        <p role="alert" data-testid="people-error">
          The people could not be read.
        </p>
      )}
      <label>
        <input
          type="checkbox"
          data-testid="show-retired"
          checked={showRetired}
          onChange={(e) => setShowRetired(e.target.checked ? true : undefined)}
        />{' '}
        Show retired
      </label>{' '}
      <label>
        Sort by{' '}
        <select data-testid="sort-by" value={sortBy} onChange={(e) => setSortBy(e.target.value as SortKey)}>
          {SORT_KEYS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </select>
      </label>
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <Col name="terms"><th>Terms</th></Col>
            <Col name="contact"><th>Contact</th></Col>
            <th>Tags</th>
            <th>Clashing</th>
            <th />
            <PersonHead.Slot sheetId={sheetId} />
          </tr>
        </thead>
        <tbody>
          {shown.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{nameWithGrade(p)}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <Col name="terms"><td data-testid="person-terms">{p.terms}</td></Col>
              <Col name="contact"><td data-testid="person-contact">{p.contact}</td></Col>
              <td data-testid="person-tags">{p.tags.join(', ')}</td>
              <td data-testid="person-clashing">{p.clashing ? 'yes' : 'no'}</td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                  Edit
                </button>
              </td>
              <PersonRow.Slot sheetId={sheetId} person={p} />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 0, Component: People });

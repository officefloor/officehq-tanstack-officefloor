import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { QuickAddBar } from './QuickAddBar';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { useSheetToday } from '../sheet/useSheet';
import { GRADES, gradeOn, peopleKey, usePeople, type Person } from './usePeople';

/** The sheet's people: one form to add a person or edit the one whose editor is open, and the list. */
function PeoplePanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: everyone = [] } = usePeople(sheet);
  // Retired people have no row unless they are shown; their claims still count in every total.
  const [showRetired] = useShowRetired();
  const people = showRetired ? everyone : everyone.filter((p) => !p.retired);
  const [editing, setEditing] = useState<number | null>(null);
  // The version of the person the editor loaded: a save is refused if someone saved since.
  const [version, setVersion] = useState(0);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  // The date the form's grade is in force from; blank for the sheet's today.
  const [gradeFrom, setGradeFrom] = useState('');
  // The person's own terms in days; blank for none.
  const [terms, setTerms] = useState('');
  // The person's contact; blank for none.
  const [contact, setContact] = useState('');
  const today = useSheetToday(sheet);
  const [blank, setBlank] = useState(false);
  const base = `/api/sheets/${encodeURIComponent(sheet)}/people`;

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (p: { id: number | null; name: string; grade: string; gradeFrom: string; terms: string; contact: string; version: number }) =>
      p.id === null
        ? postJson<Person>(base, { name: p.name, grade: p.grade, gradeFrom: p.gradeFrom, terms: p.terms, contact: p.contact, user })
        : putJson<Person>(`${base}/${p.id}`, { name: p.name, grade: p.grade, gradeFrom: p.gradeFrom, terms: p.terms, contact: p.contact, version: p.version, user }),
    onError: async (e) => {
      // Someone else saved this person since the editor loaded it: the form shows what is stored
      // now (and that version), and the list is re-read so a fresh editor loads it too.
      if (e instanceof HttpError && e.status === 409 && e.body) {
        const current = JSON.parse(e.body) as Person;
        setName(current.name);
        setGrade(gradeOn(current, today));
        setTerms(current.terms === null ? '' : String(current.terms));
        setContact(current.contact ?? '');
        setVersion(current.version);
        await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
        return;
      }
      // Any other refusal (an invalid value) keeps its error showing but closes the editor, so the
      // form is back to adding and nothing typed for the refused save carries into the next one.
      setEditing(null);
      setName('');
      setGrade('A');
      setGradeFrom('');
      setTerms('');
      setContact('');
    },
    onSuccess: async () => {
      setEditing(null);
      setName('');
      setGrade('A');
      setGradeFrom('');
      setTerms('');
      setContact('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again, or an editor is opened.
  const clearError = () => {
    save.reset();
    setBlank(false);
  };

  const open = (p: Person) => {
    clearError();
    setEditing(p.id);
    setVersion(p.version);
    setName(p.name);
    setGrade(gradeOn(p, today));
    setGradeFrom('');
    setTerms(p.terms === null ? '' : String(p.terms));
    setContact(p.contact ?? '');
  };
  const cancel = () => {
    clearError();
    setEditing(null);
    setName('');
    setGrade('A');
    setGradeFrom('');
    setTerms('');
    setContact('');
  };

  return (
    <section data-testid="people-panel">
      <h2>People</h2>
      <form
        data-testid="person-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (name.trim() === '') {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            save.reset();
            setBlank(true);
            return;
          }
          setBlank(false);
          submitOnce(() => save.mutateAsync({ id: editing, name, grade, gradeFrom: gradeFrom.trim(), terms: terms.trim(), contact: contact.trim(), version }));
        }}
      >
        <label>
          Name{' '}
          <input
            data-testid="person-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              clearError();
            }}
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
            data-testid="person-form-grade-from"
            placeholder={today || 'yyyy-mm-dd'}
            value={gradeFrom}
            onChange={(e) => {
              setGradeFrom(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          Terms{' '}
          <input
            data-testid="person-form-terms"
            placeholder="days"
            size={5}
            value={terms}
            onChange={(e) => {
              setTerms(e.target.value);
              clearError();
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
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add' : 'Save'}
        </button>
        {editing !== null && (
          <>
            {' '}
            <button type="button" data-testid="person-form-cancel" onClick={cancel}>
              Cancel
            </button>
          </>
        )}
        {(blank || save.isError) && (
          <span data-testid="person-form-error" role="alert">
            {' '}
            {blank
              ? 'A name is required.'
              : save.error instanceof HttpError && save.error.status === 409
                ? 'Someone else saved this person since you opened it; the form now shows what is stored.'
                : 'Could not save this person.'}
          </span>
        )}
      </form>
      <QuickAddBar user={user} sheet={sheet} />
      <ShowRetired />
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Terms</th>
            <th>Contact</th>
            <th />
            <PersonColumns.Slot user={user} sheet={sheet} />
          </tr>
        </thead>
        <tbody>
          {people.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{gradeOn(p, today)}</td>
              <td data-testid="person-terms">{p.terms ?? ''}</td>
              <td data-testid="person-contact">{p.contact ?? ''}</td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                  Edit
                </button>
              </td>
              <PersonCells.Slot user={user} sheet={sheet} person={p} />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 10, Component: PeoplePanel });

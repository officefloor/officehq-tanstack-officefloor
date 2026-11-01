import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { QuickAddBar } from './QuickAddBar';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { SortBy, useSortedPeople } from './SortBy';
import { useSheetToday } from '../sheet/useSheet';
import { GRADES, gradeOn, peopleKey, shownName, usePeople, type Person } from './usePeople';
import { Col } from '../columns/useColumns';
import { useCrews } from '../crews/useCrews';
import { useRoleOf } from '../settings/useSettings';

/** Whether a person has a row on this user's list: anyone's for every role but restricted. A
 *  restricted user sees the people with their name (or an alias of it, as names are compared: case
 *  and the spaces around it ignored) and the leads and members of every crew such a person leads or
 *  belongs to. No one is listed until the user's role is read. */
function useVisiblePeople(user: string, sheet: string): (id: number) => boolean {
  const role = useRoleOf(sheet, user);
  const { data: everyone = [] } = usePeople(sheet);
  const { data: crews } = useCrews(sheet);
  if (role === null) return () => false;
  if (role !== 'restricted') return () => true;
  if (crews === undefined) return () => false;
  const key = (n: string) => n.trim().toLowerCase();
  const mine = new Set(everyone.filter((p) => [p.name, ...p.aliases].some((n) => key(n) === key(user))).map((p) => p.id));
  const ids = new Set(mine);
  for (const c of crews) {
    const crewIds = [c.leadId, ...c.members.map((m) => m.personId)];
    if (crewIds.some((id) => mine.has(id))) crewIds.forEach((id) => ids.add(id));
  }
  return (id) => ids.has(id);
}

/** The sheet's people: one form to add a person or edit the one whose editor is open, and the list. */
function PeoplePanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: everyone = [] } = usePeople(sheet);
  // Retired people have no row unless they are shown; their claims still count in every total.
  const [showRetired] = useShowRetired();
  const shown = showRetired ? everyone : everyone.filter((p) => !p.retired);
  // A restricted user's list holds only their own crews' people; every other figure is over everyone.
  const visible = useVisiblePeople(user, sheet);
  const listed = shown.filter((p) => visible(p.id));
  // This user's screen may sort the list; only what is shown changes, not the list order.
  const people = useSortedPeople(sheet, listed);
  const [editing, setEditing] = useState<number | null>(null);
  // The version of the person the editor loaded: a save is refused if someone saved since.
  const [version, setVersion] = useState(0);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  // The date the form's grade is in force from; blank for the sheet's today.
  const [gradeFrom, setGradeFrom] = useState('');
  // The person's own terms in days; blank for none.
  const [terms, setTerms] = useState('');
  // The date the form's terms are in force from; blank for the beginning of time.
  const [termsFrom, setTermsFrom] = useState('');
  // The person's contact; blank for none.
  const [contact, setContact] = useState('');
  // The person's tags, comma separated; blank for none.
  const [tags, setTags] = useState('');
  const today = useSheetToday(sheet);
  const [blank, setBlank] = useState(false);
  const base = `/api/sheets/${encodeURIComponent(sheet)}/people`;

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (p: { id: number | null; name: string; grade: string; gradeFrom: string; terms: string; termsFrom: string; contact: string; tags: string; version: number }) =>
      p.id === null
        ? postJson<Person>(base, { name: p.name, grade: p.grade, gradeFrom: p.gradeFrom, terms: p.terms, termsFrom: p.termsFrom, contact: p.contact, tags: p.tags, user })
        : putJson<Person>(`${base}/${p.id}`, { name: p.name, grade: p.grade, gradeFrom: p.gradeFrom, terms: p.terms, termsFrom: p.termsFrom, contact: p.contact, tags: p.tags, version: p.version, user }),
    onError: async (e) => {
      // Someone else saved this person since the editor loaded it: the form shows what is stored
      // now (and that version), and the list is re-read so a fresh editor loads it too.
      if (e instanceof HttpError && e.status === 409 && e.body) {
        const current = JSON.parse(e.body) as Person;
        setName(current.name);
        setGrade(gradeOn(current, today));
        setTerms(current.ownTerms === null ? '' : String(current.ownTerms));
        setTermsFrom('');
        setContact(current.contact ?? '');
        setTags(current.tags.join(', '));
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
      setTermsFrom('');
      setContact('');
      setTags('');
    },
    onSuccess: async () => {
      setEditing(null);
      setName('');
      setGrade('A');
      setGradeFrom('');
      setTerms('');
      setTermsFrom('');
      setContact('');
      setTags('');
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
    setTerms(p.ownTerms === null ? '' : String(p.ownTerms));
    setTermsFrom('');
    setContact(p.contact ?? '');
    setTags(p.tags.join(', '));
  };
  const cancel = () => {
    clearError();
    setEditing(null);
    setName('');
    setGrade('A');
    setGradeFrom('');
    setTerms('');
    setTermsFrom('');
    setContact('');
    setTags('');
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
          submitOnce(() => save.mutateAsync({ id: editing, name, grade, gradeFrom: gradeFrom.trim(), terms: terms.trim(), termsFrom: termsFrom.trim(), contact: contact.trim(), tags, version }));
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
          From{' '}
          <input
            data-testid="person-form-terms-from"
            placeholder="always"
            value={termsFrom}
            onChange={(e) => {
              setTermsFrom(e.target.value);
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
        <label>
          Tags{' '}
          <input
            data-testid="person-form-tags"
            placeholder="a, b"
            value={tags}
            onChange={(e) => {
              setTags(e.target.value);
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
      <ShowRetired />{' '}
      <SortBy />
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <Col user={user} id="terms"><th>Terms</th></Col>
            <Col user={user} id="contact"><th>Contact</th></Col>
            <th>Tags</th>
            <th />
            <PersonColumns.Slot user={user} sheet={sheet} />
          </tr>
        </thead>
        <tbody>
          {people.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{shownName(p, today)}</td>
              <td data-testid="person-grade">{gradeOn(p, today)}</td>
              <Col user={user} id="terms"><td data-testid="person-terms">{p.terms ?? ''}</td></Col>
              <Col user={user} id="contact"><td data-testid="person-contact">{p.contact ?? ''}</td></Col>
              <td data-testid="person-tags">{p.tags.join(', ')}</td>
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

import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { GRADES, nameWithGrade, peopleKey, savedTermsKey, useCloseEditor, useEditingPerson, usePeople, useSavedTerms,
  useShowRetired } from './people';
import type { Person } from './people';
import { claimsKey } from '../claims/claims';
import { useSheetToday, useChangeToday } from '../sheet/sheet';
import { SORT_KEYS, useSortBy, useSortedPeople } from './sort';
import { useColumnShown } from './columns';
import { useCrews } from '../crews/crews';
import { useActingRole } from '../roles/roles.slot';
import { retiredGradeMessage, useIsGradeRetired } from '../grades/grades';

/** One form that adds a person, or edits the one whose editor is open (filled with their values). An added
 *  person's grade applies from the beginning of time; an edit records the grade from its from-date (the sheet's
 *  today unless changed), keeping the grades already recorded from other dates. The person's own terms (days; blank
 *  for none) are set by an add when given, and by an edit whose terms field was entered (blank clears them): from the
 *  edit's terms from-date when one is given (keeping those recorded from other dates), else from the beginning of time;
 *  the row shows the terms just saved until the sheet's people are next re-read (then those in force on its today); so is
 *  their contact, stored trimmed and lower-cased. Their tags (comma separated; stored trimmed, lower-cased, without
 *  repeats and sorted) are set by every add and edit, the editor filled with those stored. An add the server refuses
 *  starts the next add with no terms, no contact and no tags. A contact another person on the sheet already has is refused at once
 *  (an add's contact field is then cleared), keeping the editor open (the
 *  version it remembers brought up to date, so saving again is from the person as now stored). The
 *  editor remembers the version it loaded: when someone else has saved the person since, the save is
 *  refused and the form is refilled with the person as now stored (so saving again is from that). Any other refused
 *  edit closes the editor, leaving the person as stored and the refusal showing on the form (now adding). A grade
 *  retired on the sheet is refused at once, keeping the editor open. */
function PersonForm({ sheetId, user, editing, carried, onRefused }: { sheetId: string; user: string; editing?: Person;
  carried: string; onRefused: (error: string) => void }) {
  const queryClient = useQueryClient();
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  const [terms, setTerms] = useState(editing?.terms != null ? String(editing.terms) : '');
  const [contact, setContact] = useState(editing?.contact ?? '');
  const [tags, setTags] = useState(editing?.tags.join(', ') ?? '');
  const [termsGiven, setTermsGiven] = useState(false);
  const [contactGiven, setContactGiven] = useState(false);
  const people = usePeople(sheetId);
  const isRetired = useIsGradeRetired(sheetId);
  const closeEditor = useCloseEditor();
  const [version, setVersion] = useState(editing?.version);
  const today = useChangeToday(sheetId);
  const [gradeFromEntered, setGradeFrom] = useState<string>();
  const [termsFrom, setTermsFrom] = useState('');
  const gradeFrom = gradeFromEntered ?? today;
  const [own, showError] = useState('');
  /** The refusal of an editor just closed shows on the form (now adding) until the form is changed, submitted or
   *  an editor opened. Read as it is now, not copied as the form opens: the editor closes (a URL change) and the
   *  refusal is carried in separate updates, either of which may render first. */
  const error = own || (editing ? '' : carried);
  /** Show (or with '' remove) the form's error; removing it also drops a carried refusal. */
  const setError = (message: string) => {
    showError(message);
    if (message === '' && carried) onRefused('');
  };
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { user: string; name: string; grade: string; version?: number; gradeFrom?: string;
      terms?: string; termsFrom?: string; contact?: string; tags: string }) =>
      editing
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${editing.id}`, body)
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people`, body),
    onSuccess: async (saved, body) => {
      if (editing && body.terms !== undefined) {
        // The row shows the terms just saved until the sheet is next loaded (then those in force on its today).
        queryClient.setQueryData<Record<number, number | null>>(savedTermsKey(sheetId), (prev) =>
          ({ ...prev, [saved.id]: body.terms === '' ? null : Number(body.terms) }));
      }
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      // the person's tags join those ever used on the sheet
      void queryClient.invalidateQueries({ queryKey: [...claimsKey(sheetId), 'tags'] });
      setName('');
      setGrade('A');
      setTerms('');
      setTermsFrom('');
      setContact('');
      setTags('');
      // Only this person's editor: another may have been opened while the save was on its way.
      if (editing) closeEditor(editing.id);
    },
    onError: async (e) => {
      if (editing && e instanceof HttpError && e.status === 412) {
        const people = await getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`);
        queryClient.setQueryData(peopleKey(sheetId), people);
        const current = people.find((p) => p.id === editing.id);
        if (current) {
          setName(current.name);
          setGrade(current.grade);
          setTerms(current.terms != null ? String(current.terms) : '');
          setContact(current.contact ?? '');
          setTags(current.tags.join(', '));
          setVersion(current.version);
        }
      }
      // An add the server refused starts the next add with no terms, no contact and no tags.
      if (!editing) {
        setTerms('');
        setContact('');
        setTags('');
      }
      refuse(
        e instanceof HttpError && (e.status === 409 || e.status === 412) && e.body
          ? e.body
          : 'Could not save this person: a name is required, the grade must be A, B or C, the from-date a date and the terms blank or a whole number of days.',
        e instanceof HttpError && e.status === 412,
      );
    },
    onSettled: once.done,
  });

  /** Show the refusal; a refused edit (other than a stale one, refilled to save again) closes the editor. */
  const refuse = (message: string, stale = false) => {
    if (editing && !stale) {
      onRefused(message);
      setEditing(undefined);
    } else {
      setError(message);
    }
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      refuse('A name is required.');
      return;
    }
    if (!/^\d{0,4}$/.test(terms.trim())) {
      refuse('The terms must be a whole number of days, 0 or more, or blank for none.');
      return;
    }
    // A retired grade cannot be chosen for a new person or in an edit: refused at once, keeping the editor open.
    if (isRetired(grade)) {
      setError(retiredGradeMessage(grade));
      return;
    }
    const key = contact.trim().toLowerCase();
    if (key !== '' && (!editing || contactGiven) && (people.data ?? []).some((p) => p.id !== editing?.id && p.contact === key)) {
      setError(`The contact ${key} is already in use on this sheet.`);
      // An add refused for its contact starts the next add with no contact.
      if (!editing) setContact('');
      if (editing) {
        void getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`).then((latest) => {
          queryClient.setQueryData(peopleKey(sheetId), latest);
          const current = latest.find((p) => p.id === editing.id);
          if (current) setVersion(current.version);
        });
      }
      return;
    }
    if (!once.start()) return;
    save.mutate(
      editing
        ? { user, name, grade, version, gradeFrom: gradeFrom.trim() === '' ? undefined : gradeFrom, terms: termsGiven ? terms.trim() : undefined,
            termsFrom: termsGiven && termsFrom.trim() !== '' ? termsFrom.trim() : undefined,
            contact: contactGiven ? key : undefined, tags }
        : { user, name, grade, terms: terms.trim() === '' ? undefined : terms.trim(), contact: key === '' ? undefined : key, tags },
    );
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
            setGrade(e.target.value);
            setError('');
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
        Terms (days){' '}
        <input
          data-testid="person-form-terms"
          value={terms}
          onKeyDown={() => setTermsGiven(true)}
          // the same value entered again fires no change, but is still entered
          onInput={() => setTermsGiven(true)}
          onChange={(e) => {
            setTerms(e.target.value);
            setTermsGiven(true);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Contact{' '}
        <input
          data-testid="person-form-contact"
          value={contact}
          onKeyDown={() => setContactGiven(true)}
          onInput={() => setContactGiven(true)}
          onChange={(e) => {
            setContact(e.target.value);
            setContactGiven(true);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Tags{' '}
        <input
          data-testid="person-form-tags"
          placeholder="Tags (comma separated)"
          value={tags}
          onChange={(e) => {
            setTags(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      {editing && (
        <>
          <label>
            Grade from{' '}
            <input
              type="date"
              data-testid="person-form-grade-from"
              value={gradeFrom}
              onChange={(e) => {
                setGradeFrom(e.target.value);
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
        </>
      )}
      <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
        {editing ? 'Save' : 'Add person'}
      </button>
      {editing && (
        <button type="button" data-testid="person-form-cancel" onClick={() => {
          onRefused('');
          setEditing(undefined);
        }}>
          Cancel
        </button>
      )}
      {error && (
        <p data-testid="person-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The quick-add bar: a name and an amount make a DRAFT claim dated the sheet's entry date (while it is set, else its today) with an empty note for
 *  the person of that name, adding them first with the sheet's default grade (A when blank) when the name is new to the sheet. A refusal adds
 *  nothing and shows why on the bar. */
function QuickAddBar({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { user: string; name: string; amount: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/quick-add`, body),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      setName('');
      setAmount('');
    },
    onError: (e) =>
      setError(
        e instanceof HttpError && e.body
          ? e.body
          : 'Could not quick-add: a name is required and the amount must be a number, 0.00 or more, with up to two decimals.',
      ),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('A name is required.');
      return;
    }
    if (!/^\d{1,12}(\.\d{1,2})?$/.test(amount.trim())) {
      setError('The amount must be a number, 0.00 or more, with up to two decimals.');
      return;
    }
    if (!once.start()) return;
    add.mutate({ user, name, amount });
  };

  return (
    <form data-testid="quick-add" onSubmit={submit}>
      <label>
        Name{' '}
        <input
          data-testid="quick-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        Amount{' '}
        <input
          data-testid="quick-amount"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Quick add
      </button>
      {error && (
        <p data-testid="quick-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The people this user's people list may show: everyone, unless the user acts as restricted — then only the
 *  person with their name and the leads and members of the crews that person leads or belongs to. */
function useVisiblePeople(sheetId: string, user: string, people: Person[]): Person[] {
  const restricted = useActingRole(sheetId, user) === 'restricted';
  const crews = useCrews(sheetId).data ?? [];
  if (!restricted) return people;
  const self = people.filter((p) => p.name === user.trim()).map((p) => p.id);
  const ids = new Set(self);
  for (const crew of crews) {
    const ofCrew = [crew.leadId, ...crew.members.map((m) => m.personId)];
    if (ofCrew.some((id) => self.includes(id))) ofCrew.forEach((id) => ids.add(id));
  }
  return people.filter((p) => ids.has(p.id));
}

/** The sheet's people, in list order unless this user's screen sorts them otherwise, each with their name and
 *  grade. Retired people have no row unless "show retired" is on. A restricted user sees only the person with their
 *  name and the people of the crews that person leads or belongs to (no row at all when no person has their name). */
function People({ sheetId, user }: { sheetId: string; user: string }) {
  const today = useSheetToday(sheetId);
  const people = usePeople(sheetId);
  const [editingId, setEditing] = useEditingPerson();
  const editing = people.data?.find((p) => String(p.id) === editingId);
  const [showRetired, setShowRetired] = useShowRetired();
  const [carried, setCarried] = useState('');
  const [sortBy, setSortBy] = useSortBy();
  const showTerms = useColumnShown('terms');
  const savedTerms = useSavedTerms(sheetId);
  const queryClient = useQueryClient();
  // Loading another sheet drops the terms this screen just saved on this one.
  useEffect(() => () => queryClient.removeQueries({ queryKey: savedTermsKey(sheetId) }), [queryClient, sheetId]);
  const showContact = useColumnShown('contact');
  const visible = useVisiblePeople(sheetId, user, people.data ?? []);
  const listed = useSortedPeople(sheetId, visible.filter((p) => showRetired || p.retiredFrom === null));

  return (
    <section data-testid="people">
      <h2>People</h2>
      {/* Keyed by who is being edited, so opening an editor fills the form with their values. */}
      <PersonForm key={editing ? editing.id : 'new'} sheetId={sheetId} user={user} editing={editing}
        carried={carried} onRefused={setCarried} />
      <QuickAddBar sheetId={sheetId} user={user} />
      <label>
        <input
          type="checkbox"
          data-testid="show-retired"
          checked={showRetired}
          onChange={(e) => setShowRetired(e.target.checked || undefined)}
        />{' '}
        Show retired
      </label>{' '}
      <label>
        Sort by{' '}
        <select data-testid="sort-by" value={sortBy} onChange={(e) => setSortBy(e.target.value as typeof sortBy)}>
          {SORT_KEYS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </select>
      </label>
      <table>
        <tbody>
          {listed.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{nameWithGrade(p, today)}</td>
              <td data-testid="person-grade">{p.grade}</td>
              {showTerms && <td data-testid="person-terms">{(p.id in savedTerms ? savedTerms[p.id] : p.terms) ?? ''}</td>}
              {showContact && <td data-testid="person-contact">{p.contact ?? ''}</td>}
              <td data-testid="person-tags">{p.tags.join(', ')}</td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => {
                  setCarried('');
                  setEditing(String(p.id));
                }}>
                  Edit
                </button>
              </td>
              <PersonRow.Slot sheetId={sheetId} user={user} person={p} />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: People });

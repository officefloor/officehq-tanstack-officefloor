import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { GRADES, isRetired, peopleKey, peoplePath, usePeople, useShowRetired } from './people';
import type { Grade, Person } from './people';
import { SORT_KEYS, useSorted, useSortBy } from './sort';
import type { SortKey } from './sort';
import { claimsKey } from '../claims/claims';
import { useShown } from '../columns/columns';
import type { Claim } from '../claims/claims';

// gradeFrom is the date an edit records the grade from (yyyy-mm-dd; blank for the sheet's today); a person's
// first grade applies from the beginning of time. terms are the person's own days (blank for none); contact is
// theirs, not another's on the sheet (blank for none); tags are comma separated (the server trims, lower-cases and
// drops repeats).
type Draft = { name: string; grade: Grade; gradeFrom: string; terms: string; contact: string; tags: string };
const EMPTY: Draft = { name: '', grade: 'A', gradeFrom: '', terms: '', contact: '', tags: '' };
const termsText = (p: Person) => (p.terms === null ? '' : String(p.terms));

type Quick = { name: string; amount: string };
const QUICK_EMPTY: Quick = { name: '', amount: '' };

// The quick-add bar: a name and an amount make a draft claim dated the sheet's today with an empty
// note, for the person of that name (ignoring case), who is first added with grade A when not on the
// sheet. The server checks every rule before writing, so a refusal adds no one.
function QuickAdd({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState<Quick>(QUICK_EMPTY);

  const add = useMutation({
    mutationFn: (body: Quick) =>
      postJson<{ people: Person[]; claims: Claim[] }>(
        byUser(`/api/sheets/${encodeURIComponent(sheet)}/quick-add`, user),
        body,
      ),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated.people);
      queryClient.setQueryData(claimsKey(sheet), updated.claims);
      setDraft(QUICK_EMPTY);
    },
  });
  const send = useSubmitOnce(add);

  const change = (next: Partial<Quick>) => {
    setDraft((d) => ({ ...d, ...next }));
    add.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(draft);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Quick add: name{' '}
        <input data-testid="quick-name" value={draft.name} onChange={(e) => change({ name: e.target.value })} />
      </label>{' '}
      <label>
        Amount{' '}
        <input data-testid="quick-amount" value={draft.amount} onChange={(e) => change({ amount: e.target.value })} />
      </label>{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add claim
      </button>
      {add.isError && (
        <span data-testid="quick-error" role="alert">
          {' '}
          A quick add needs a name and an amount of 0.00 or more with up to two decimals, dated outside any closed
          period.
        </span>
      )}
    </form>
  );
}

// The sheet's people: listed by position, new ones at the end (retired ones only when shown), with one form that adds a person or,
// once a person's editor is opened, edits them. The editor remembers the version of the person it loaded;
// when someone else has saved them since, the save is refused and the form shows their current values.
function People({ sheet, user }: { sheet: string; user: string }) {
  const shown = useShown(sheet, user);
  const queryClient = useQueryClient();
  const { data: everyone = [] } = usePeople(sheet);
  const [showRetired, setShowRetired] = useShowRetired();
  // Retired people have no row at all unless "show retired" is on; their claims still count in every total.
  const listed = showRetired ? everyone : everyone.filter((p) => !isRetired(p));
  // The rows as this user's screen sorts them; list order itself is unchanged.
  const people = useSorted(sheet, listed);
  const [sortBy, setSortBy] = useSortBy();
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [editing, setEditing] = useState<{ id: number; version: number } | null>(null);

  const save = useMutation({
    mutationFn: ({ edit, body }: { edit: { id: number; version: number } | null; body: Draft }) =>
      edit === null
        ? postJson<Person[]>(byUser(peoplePath(sheet), user), body)
        : putJson<Person[]>(byUser(`${peoplePath(sheet)}/${edit.id}`, user), { ...body, version: edit.version }),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
      setDraft(EMPTY);
      setEditing(null);
    },
    onError: async (error, { edit }) => {
      if (edit === null) return;
      if (!(error instanceof HttpError) || error.status !== 412) {
        // A refused edit writes nothing and closes the editor, leaving its error shown, so the form adds again.
        setDraft(EMPTY);
        setEditing(null);
        return;
      }
      // Someone else saved the person: show what is stored now, and edit from that version.
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      const p = queryClient.getQueryData<Person[]>(peopleKey(sheet))?.find((c) => c.id === edit.id);
      if (p) {
        setDraft({ name: p.name, grade: p.grade, gradeFrom: '', terms: termsText(p), contact: p.contact ?? '', tags: p.tags.join(', ') });
        setEditing({ id: p.id, version: p.version });
      }
    },
  });
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const open = (p: Person) => {
    setEditing({ id: p.id, version: p.version });
    setDraft({ name: p.name, grade: p.grade, gradeFrom: '', terms: termsText(p), contact: p.contact ?? '', tags: p.tags.join(', ') });
    save.reset();
  };

  const cancel = () => {
    setEditing(null);
    setDraft(EMPTY);
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses an empty name, one already on the sheet, or a save over someone else's;
    // its refusal shows as the form's error.
    send({ edit: editing, body: draft });
  };

  return (
    <section>
      <h2>People</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input data-testid="person-form-name" value={draft.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={draft.grade}
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
            data-testid="person-form-grade-from"
            placeholder="yyyy-mm-dd"
            value={draft.gradeFrom}
            onChange={(e) => change({ gradeFrom: e.target.value })}
          />
        </label>{' '}
        <label>
          Terms{' '}
          <input
            data-testid="person-form-terms"
            placeholder="days"
            value={draft.terms}
            onChange={(e) => change({ terms: e.target.value })}
          />
        </label>{' '}
        <label>
          Contact{' '}
          <input
            data-testid="person-form-contact"
            value={draft.contact}
            onChange={(e) => change({ contact: e.target.value })}
          />
        </label>{' '}
        <label>
          Tags{' '}
          <input
            data-testid="person-form-tags"
            placeholder="tags, comma separated"
            value={draft.tags}
            onChange={(e) => change({ tags: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add person' : 'Save person'}
        </button>
        {editing !== null && (
          <button type="button" data-testid="person-form-cancel" onClick={cancel}>
            Cancel
          </button>
        )}
        {save.isError && (
          <span data-testid="person-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.status === 412
              ? 'Someone else saved this person since you opened it; the form now shows their values.'
              : 'A person needs a name not already on the sheet or its tree (and, when clashing, a new name and aliases that no longer clash), a grade of A, B or C, a from-date that is a valid date or blank, terms that are a whole number of days or blank, and a contact not already another person\'s on the sheet.'}
          </span>
        )}
      </form>
      <QuickAdd sheet={sheet} user={user} />
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
        <select
          data-testid="sort-by"
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value === 'added' ? undefined : (e.target.value as SortKey))}
        >
          {SORT_KEYS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </select>
      </label>
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            {shown('terms') && <th>Terms</th>}
            {shown('contact') && <th>Contact</th>}
            <th>Tags</th>
            <th>Clashing</th>
            <th>From sheet</th>
            <PersonColumnHeaders.Slot sheet={sheet} user={user} />
            <th />
          </tr>
        </thead>
        <tbody>
          {people.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              {shown('terms') && <td data-testid="person-terms">{termsText(p)}</td>}
              {shown('contact') && <td data-testid="person-contact">{p.contact ?? ''}</td>}
              <td data-testid="person-tags">{p.tags.join(', ')}</td>
              <td data-testid="person-clashing">{p.clashing ? 'yes' : 'no'}</td>
              <td data-testid="person-origin">{p.origin ?? ''}</td>
              <PersonColumns.Slot sheet={sheet} user={user} person={p} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 10, Component: People });

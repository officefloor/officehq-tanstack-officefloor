import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { claimsKey, type Claim } from '../claims/claims';
import { tagsKey } from '../tags/tags';
import { usePeopleDue } from '../header/totals';
import { GRADES, peopleKey, SORT_KEYS, sortPeople, useEditingPerson, usePeople, useShowRetired, useSortBy, type Person } from './people';
import { useShownColumns } from '../columns/columns';

/** The form's values. `gradeFrom` (yyyy-MM-dd) is the date the grade is given from; blank, the sheet's today. A
 *  person's first grade applies from the beginning of time. `terms` are the person's own terms in days; blank, none. `termsFrom` (yyyy-MM-dd) is the date they are given from;
 *  blank, the beginning of time. `contact` is the person's contact, kept by the server
 *  without surrounding spaces and lower-cased; blank, none. `tags` are the person's tags, comma separated, kept by the server
 *  each trimmed and lower-cased, repeats dropped; blank, none. */
type Values = { name: string; grade: string; gradeFrom: string; terms: string; termsFrom: string; contact: string; tags: string };
const EMPTY: Values = { name: '', grade: 'A', gradeFrom: '', terms: '', termsFrom: '', contact: '', tags: '' };

/** The form's values for a person as stored. */
const valuesOf = (p: Person): Values => ({
  name: p.name,
  grade: p.grade,
  gradeFrom: '',
  terms: p.terms === null ? '' : String(p.terms),
  termsFrom: '',
  contact: p.contact ?? '',
  tags: (p.tags ?? []).join(', '),
});

type Quick = { name: string; amount: string };
const NO_QUICK: Quick = { name: '', amount: '' };

/** The quick-add bar: a DRAFT claim of the amount, dated the sheet's today with an empty note, for the person with the
 *  name (case and outer spaces ignored); a name not on the sheet adds the person, grade A, first. The server does it as
 *  one change, so a refused claim adds no person. */
function QuickAdd({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Quick>(NO_QUICK);
  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (v: Quick) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick-add`, { ...v, by: user }),
    onSuccess: () => {
      setValues(NO_QUICK);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const change = (v: Partial<Quick>) => {
    setValues((was) => ({ ...was, ...v }));
    add.reset();
  };

  return (
    <form
      data-testid="quick-add"
      onSubmit={(e) => {
        e.preventDefault();
        if (!submit.begin()) return;
        add.reset();
        add.mutate(values);
      }}
    >
      Quick add{' '}
      <input data-testid="quick-name" placeholder="Name" value={values.name} onChange={(e) => change({ name: e.target.value })} />{' '}
      <input
        data-testid="quick-amount"
        placeholder="Amount"
        inputMode="decimal"
        value={values.amount}
        onChange={(e) => change({ amount: e.target.value })}
      />{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add draft claim
      </button>
      {add.isError && (
        <span data-testid="quick-error">
          {' '}
          {values.name.trim() === ''
            ? 'A name is required.'
            : "The amount must be 0.00 or more, with up to two decimals, its due-by not inside a closed period, and not after the person's retirement date."}
        </span>
      )}
    </form>
  );
}

/** The sheet's people: a form that adds a person or saves the one being edited, and the list. The editor
 *  remembers the version of the person it loaded; a save refused because someone else saved the person since
 *  (412) shows the error and puts the current values, and their version, in the form; an edit refused otherwise shows the
 *  error and closes the editor. */
function People({ sheetId, user }: { sheetId: string; user: string }) {
  const shown = useShownColumns(user);
  const people = usePeople(sheetId);
  const [showRetired, setShowRetired] = useShowRetired();
  const [sortBy, setSortBy] = useSortBy();
  const due = usePeopleDue(sheetId);
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useEditingPerson();
  const editing = people.data?.find((p) => String(p.id) === editingId);
  const [values, setValues] = useState<Values>(EMPTY);
  const [version, setVersion] = useState<number | undefined>(undefined);
  const submit = useSubmitOnce();

  const save = useMutation({
    mutationFn: ({ id, ...v }: Values & { id?: number; version?: number }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
      const body = { ...v, by: user };
      return id === undefined ? postJson<Person>(base, body) : putJson<Person>(`${base}/${id}`, body);
    },
    onSuccess: () => {
      setValues(EMPTY);
      setEditingId(undefined);
      setVersion(undefined);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: tagsKey(sheetId) });
    },
    onError: async (error, { id }) => {
      if (id === undefined) return;
      if (!(error instanceof HttpError) || error.status !== 412) {
        // a refused edit closes the editor, its error still showing: the form is back to adding
        setValues(EMPTY);
        setVersion(undefined);
        setEditingId(undefined);
        return;
      }
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      const now = queryClient.getQueryData<Person[]>(peopleKey(sheetId))?.find((p) => p.id === id);
      if (now) {
        setValues(valuesOf(now));
        setVersion(now.version);
      }
    },
    onSettled: submit.end,
  });

  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
  };
  const openEditor = (p: Person) => {
    save.reset();
    setValues(valuesOf(p));
    setVersion(p.version);
    setEditingId(String(p.id));
  };
  const cancel = () => {
    save.reset();
    setValues(EMPTY);
    setVersion(undefined);
    setEditingId(undefined);
  };

  return (
    <section data-testid="people">
      <h3>People</h3>
      <form
        data-testid="person-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate(editing ? { ...values, id: editing.id, version } : values);
        }}
      >
        <label>
          Name{' '}
          <input data-testid="person-form-name" value={values.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          Grade{' '}
          <select data-testid="person-form-grade" value={values.grade} onChange={(e) => change({ grade: e.target.value })}>
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
            value={values.gradeFrom}
            onChange={(e) => change({ gradeFrom: e.target.value })}
          />
        </label>{' '}
        <label>
          Terms{' '}
          <input
            data-testid="person-form-terms"
            placeholder="days"
            inputMode="numeric"
            value={values.terms}
            onChange={(e) => change({ terms: e.target.value })}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="person-form-terms-from"
            placeholder="yyyy-mm-dd"
            value={values.termsFrom}
            onChange={(e) => change({ termsFrom: e.target.value })}
          />
        </label>{' '}
        <label>
          Contact{' '}
          <input data-testid="person-form-contact" value={values.contact} onChange={(e) => change({ contact: e.target.value })} />
        </label>{' '}
        <label>
          Tags{' '}
          <input
            data-testid="person-form-tags"
            placeholder="comma separated"
            value={values.tags}
            onChange={(e) => change({ tags: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing ? 'Save' : 'Add person'}
        </button>
        {editing && (
          <button type="button" data-testid="person-form-cancel" onClick={cancel}>
            Cancel
          </button>
        )}
        {save.isError && (
          <span data-testid="person-form-error">
            {(save.variables?.name ?? '').trim() === ''
              ? 'A name is required.'
              : save.error instanceof HttpError && save.error.status === 409
                ? 'That name or contact is already in use on this sheet.'
                : save.error instanceof HttpError && save.error.status === 412
                  ? 'Someone else saved this person since you opened it; the form now shows their values.'
                  : 'Could not save this person.'}
          </span>
        )}
      </form>
      <QuickAdd sheetId={sheetId} user={user} />
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
        <select data-testid="sort-by" value={sortBy || 'added'} onChange={(e) => setSortBy(e.target.value === 'added' ? undefined : e.target.value)}>
          {SORT_KEYS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </select>
      </label>
      {people.isError && <p data-testid="people-load-error">Could not load people.</p>}
      <table data-testid="people-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            {shown('terms') && <th>Terms</th>}
            {shown('contact') && <th>Contact</th>}
            <th>Tags</th>
            <PeopleHead.Slot sheetId={sheetId} user={user} />
            <th />
          </tr>
        </thead>
        <tbody>
          {sortPeople(people.data?.filter((p) => showRetired || p.retiredFrom === null) ?? [], sortBy, due).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              {shown('terms') && <td data-testid="person-terms">{p.terms ?? ''}</td>}
              {shown('contact') && <td data-testid="person-contact">{p.contact ?? ''}</td>}
              <td data-testid="person-tags">{(p.tags ?? []).join(', ')}</td>
              <PersonRow.Slot sheetId={sheetId} user={user} person={p} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => openEditor(p)}>
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

export const contribution = SheetPage.fill({ order: 10, Component: People });

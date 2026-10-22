import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { GRADES, peopleKey, useEditingPerson, usePeople, useShowRetired } from './people';
import type { Person } from './people';
import { claimsKey } from '../claims/claims';

/** One form that adds a person, or edits the one whose editor is open (filled with their values). The
 *  editor remembers the version it loaded: when someone else has saved the person since, the save is
 *  refused and the form is refilled with the person as now stored (so saving again is from that). */
function PersonForm({ sheetId, user, editing }: { sheetId: string; user: string; editing?: Person }) {
  const queryClient = useQueryClient();
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  const [version, setVersion] = useState(editing?.version);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { user: string; name: string; grade: string; version?: number }) =>
      editing
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${editing.id}`, body)
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setName('');
      setGrade('A');
      if (editing) setEditing(undefined);
    },
    onError: async (e) => {
      if (editing && e instanceof HttpError && e.status === 412) {
        const people = await getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`);
        queryClient.setQueryData(peopleKey(sheetId), people);
        const current = people.find((p) => p.id === editing.id);
        if (current) {
          setName(current.name);
          setGrade(current.grade);
          setVersion(current.version);
        }
      }
      setError(
        e instanceof HttpError && (e.status === 409 || e.status === 412) && e.body
          ? e.body
          : 'Could not save this person: a name is required and the grade must be A, B or C.',
      );
    },
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('A name is required.');
      return;
    }
    if (!once.start()) return;
    save.mutate({ user, name, grade, version: editing ? version : undefined });
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
      <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
        {editing ? 'Save' : 'Add person'}
      </button>
      {editing && (
        <button type="button" data-testid="person-form-cancel" onClick={() => setEditing(undefined)}>
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

/** The quick-add bar: a name and an amount make a DRAFT claim dated the sheet's today with an empty note for
 *  the person of that name, adding them first with grade A when the name is new to the sheet. A refusal adds
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

/** The sheet's people in the order they were added, each with their name and grade. Retired people have no
 *  row unless "show retired" is on. */
function People({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const [editingId, setEditing] = useEditingPerson();
  const editing = people.data?.find((p) => String(p.id) === editingId);
  const [showRetired, setShowRetired] = useShowRetired();
  const listed = (people.data ?? []).filter((p) => showRetired || p.retiredFrom === null);

  return (
    <section data-testid="people">
      <h2>People</h2>
      {/* Keyed by who is being edited, so opening an editor fills the form with their values. */}
      <PersonForm key={editing ? editing.id : 'new'} sheetId={sheetId} user={user} editing={editing} />
      <QuickAddBar sheetId={sheetId} user={user} />
      <label>
        <input
          type="checkbox"
          data-testid="show-retired"
          checked={showRetired}
          onChange={(e) => setShowRetired(e.target.checked || undefined)}
        />{' '}
        Show retired
      </label>
      <table>
        <tbody>
          {listed.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(String(p.id))}>
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

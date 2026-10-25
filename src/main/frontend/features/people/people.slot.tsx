import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, postJson, putJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { useUser } from '../session/useSession';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { claimsKey, type Claim } from '../claims/useClaims';

/** `version` counts the person's saves; an edit sends the one its form loaded. `statedTotal` ("n/a" when it had
 *  none) and `statementDate` are the last statement's, none before the first. */
export type Person = { id: number; name: string; grade: string; version: number;
  statementCount: number; statedTotal: string | null; statementDate: string | null };

const GRADES = ['A', 'B', 'C'] as const;

/** The key every feature reads the sheet's people under. */
export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
  });
}

/** Which person's editor is open (search param `editPerson`); none means the form adds. */
export function useEditingPerson() {
  return useSearchParam('editPerson', asNumber);
}

// The person form: adds a person, or — with a person's editor open — saves their changes. It is
// re-mounted for each person opened (see PeoplePanel), so it starts filled with their values and
// remembers the version it loaded. When someone else has saved the person since, the save is
// refused: the form then shows the person's current values and version, so saving again works.
function PersonForm({ sheetId, editing }: { sheetId: string; editing?: Person }) {
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  const [version, setVersion] = useState(editing?.version);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
  const save = useMutation({
    mutationFn: async (body: { name: string; grade: string }) => {
      if (body.name.trim() === '') throw new Error('Enter a name');
      // `by` is the signed-in user the change is recorded against.
      const sent = { ...body, by: user ?? '' };
      return editing
        ? putJson<Person>(`${base}/${editing.id}`, { ...sent, version })
        : postJson<Person>(base, sent);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      if (editing) {
        setEditing(undefined);
      } else {
        setName('');
        setGrade('A');
      }
    },
    onError: async (error) => {
      if (!editing || !(error instanceof HttpError) || error.status !== 409) return;
      // Someone else saved this person: re-read the sheet's people and show their current values.
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      const now = queryClient.getQueryData<Person[]>(peopleKey(sheetId))?.find((p) => p.id === editing.id);
      if (now) {
        setName(now.name);
        setGrade(now.grade);
        setVersion(now.version);
      }
    },
  });

  const submit = useSubmitOnce(save);
  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ name, grade });
      }}
    >
      <label>
        Name{' '}
        <input
          data-testid="person-form-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            save.reset();
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
            save.reset();
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
        <>
          {' '}
          <button type="button" data-testid="person-form-cancel" onClick={() => setEditing(undefined)}>
            Cancel
          </button>
        </>
      )}
      {save.isError && (
        <p data-testid="person-form-error" role="alert">
          {save.error.message}
        </p>
      )}
    </form>
  );
}

// The quick-add bar: a name and an amount make a draft claim dated the sheet's today, with an empty
// note, for the person of that name (case and surrounding spaces ignored) — adding them, grade A,
// when the name is not on the sheet. The server checks it all as one change, so a refusal adds no one.
function QuickAddBar({ sheetId }: { sheetId: string }) {
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (body: { name: string; amount: string }) => {
      if (body.name.trim() === '') throw new Error('Enter a name');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick-add`, { ...body, by: user ?? '' });
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      setName('');
      setAmount('');
    },
  });

  const submit = useSubmitOnce(add);
  return (
    <form
      data-testid="quick-add"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ name, amount });
      }}
    >
      <label>
        Quick add: name{' '}
        <input
          data-testid="quick-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />
      </label>{' '}
      <label>
        Amount{' '}
        <input
          data-testid="quick-amount"
          inputMode="decimal"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            add.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add draft claim
      </button>
      {add.isError && (
        <p data-testid="quick-error" role="alert">
          {add.error.message}
        </p>
      )}
    </form>
  );
}

// The people on the sheet, in the order they were added, and the form that adds or edits them.
function PeoplePanel({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const [editingId, setEditing] = useEditingPerson();
  const editing = people.data?.find((p) => p.id === editingId);

  return (
    <section data-testid="people-panel">
      <h2>People</h2>
      <PersonForm key={editing ? `edit-${editing.id}` : 'add'} sheetId={sheetId} editing={editing} />
      <QuickAddBar sheetId={sheetId} />
      {people.isError && (
        <p data-testid="people-error" role="alert">
          {people.error.message}
        </p>
      )}
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PersonHeads.Slot sheetId={sheetId} user={user} />
            <th />
          </tr>
        </thead>
        <tbody>
          {(people.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <PersonCells.Slot sheetId={sheetId} user={user} personId={p.id} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
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

export const contribution = SheetBody.fill({ order: 10, Component: PeoplePanel });

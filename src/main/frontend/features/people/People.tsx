import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { useCurrentUser } from '../session/SignIn';
import { ClaimedTotal, PersonClaims, claimed, useClaims } from '../claims/Claims';
import { due, rateOf, total, useRates } from '../rates/Rates';

// The people on the sheet, in the order they were added. One form adds a person, or edits the
// person whose editor is open (URL state `editPerson`, the person's id).
export type Person = { id: number; name: string; grade: string };

const GRADES = ['A', 'B', 'C'];

export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'];
const peoplePath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/people`;

export function usePeople(sheet: string): Person[] {
  return (
    useQuery({
      queryKey: peopleKey(sheet),
      queryFn: () => getJson<Person[]>(peoplePath(sheet)),
      enabled: !!sheet,
    }).data ?? []
  );
}

function useEditPerson() {
  return useSearchParam('editPerson', asNumber);
}

export function People({ sheet }: { sheet: string }) {
  const people = usePeople(sheet);
  const [editId, setEditId] = useEditPerson();
  const editing = people.find((p) => p.id === editId);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const claimsOf = (p: Person) => claims.filter((c) => c.personId === p.id);
  // A person's due-total is on their claimed total, not the sum of their claims' rounded dues.
  const dueTotalOf = (p: Person) => due(claimed(claimsOf(p)), rateOf(rates, p.grade));

  return (
    <section>
      <h2>People</h2>
      {/* Keyed so opening another person's editor (or closing it) refills the form. */}
      <PersonForm key={editing ? `edit-${editing.id}` : 'add'} sheet={sheet} editing={editing}
        onDone={() => setEditId(undefined)} />
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Claimed</th>
            <th>Due</th>
            <th>Claims</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {people.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <td data-testid="person-claimed">{claimed(claimsOf(p))}</td>
              <td data-testid="person-due-total">{dueTotalOf(p)}</td>
              <td>
                <PersonClaims sheet={sheet} personId={p.id} claims={claimsOf(p)} pct={rateOf(rates, p.grade)} />
              </td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditId(p.id)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <ClaimedTotal sheet={sheet} />
      <p>
        Due total: <strong data-testid="rollup-due-total">{total(people.map(dueTotalOf))}</strong>
      </p>
    </section>
  );
}

function PersonForm({ sheet, editing, onDone }: { sheet: string; editing?: Person; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const save = useMutation({
    mutationFn: (body: { name: string; grade: string }) =>
      editing
        ? putJson<Person>(`${peoplePath(sheet)}/${editing.id}${by}`, body)
        : postJson<Person>(`${peoplePath(sheet)}${by}`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      if (editing) {
        onDone();
      } else {
        setName('');
        setGrade('A');
      }
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        // A blank or already-used name is refused by the server (400), which shows the form's error.
        save.mutate({ name, grade });
      }}
    >
      <label>
        Name{' '}
        <input
          data-testid="person-form-name"
          value={name}
          onChange={(e) => {
            save.reset();
            setName(e.target.value);
          }}
        />
      </label>{' '}
      <label>
        Grade{' '}
        <select
          data-testid="person-form-grade"
          value={grade}
          onChange={(e) => {
            save.reset();
            setGrade(e.target.value);
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
        {editing ? 'Save' : 'Add'}
      </button>
      {editing && (
        <button type="button" onClick={onDone}>
          Cancel
        </button>
      )}
      {save.isError && (
        <span role="alert" data-testid="person-form-error">
          {' '}
          {(save.error instanceof HttpError && save.error.status === 400 && save.error.reason)
            || 'The person could not be saved.'}
        </span>
      )}
    </form>
  );
}

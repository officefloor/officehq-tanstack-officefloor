import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { useCurrentUser } from '../session/SignIn';
import { ClaimedTotal, PersonClaims, claimDue, claimed, claimsKey, counts, hasFixedDue, useClaims } from '../claims/Claims';
import { NA, rateOn, total, useRates } from '../rates/Rates';
import { useSheetToday } from '../sheet/SheetToday';

// The people on the sheet, in the order they were added. One form adds a person, or edits the
// person whose editor is open (URL state `editPerson`, the person's id).
// A statement taken for a person records their due-total and binds their counting claims' figures.
export type Person = { id: number; name: string; grade: string; version: number;
  /** How many statements have been taken for the person. */
  statementCount: number;
  /** The last statement's due-total; null when none was taken, or it was n/a. */
  statedTotal: string | null;
  /** The sheet's today when the last statement was taken; null when none was. */
  statementDate: string | null };

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
  // Counts each press of an Edit button, so opening the editor again reloads it with the current values.
  const [opened, setOpened] = useState(0);
  const editing = people.find((p) => p.id === editId);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const claimsOf = (p: Person) => claims.filter((c) => c.personId === p.id);
  // A person's due-total is the sum of their counted claims' already-rounded figures (0.00 with none).
  // It is n/a when their grade has no rate on the sheet's today, unless a counted claim keeps a figure
  // (settled, or bound by a statement): then it is the sum of the figures there are.
  const dueTotalOf = (p: Person) => {
    const counted = claimsOf(p).filter(counts);
    if (rateOn(rates, p.grade, today) === undefined && !counted.some(hasFixedDue)) return NA;
    return total(counted.map((c) => claimDue(c, rateOn(rates, p.grade, c.date))));
  };

  return (
    <section>
      <h2>People</h2>
      {/* Keyed so opening a person's editor (again) or closing it refills the form. */}
      <PersonForm key={editing ? `edit-${editing.id}-${opened}` : 'add'} sheet={sheet} editing={editing}
        onDone={() => setEditId(undefined)} />
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Claimed</th>
            <th>Due</th>
            <th>Statements</th>
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
                <PersonStatement sheet={sheet} person={p} />
              </td>
              <td>
                <PersonClaims sheet={sheet} personId={p.id} claims={claimsOf(p)}
                  pctOn={(date) => rateOn(rates, p.grade, date)} />
              </td>
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => { setOpened((n) => n + 1); setEditId(p.id); }}>
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

/** The person's statements: how many, the last one's due-total and date, and the button that takes
 *  one. A refused statement shows the row's error, which the next statement clears. */
function PersonStatement({ sheet, person: p }: { sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const take = useMutation({
    mutationFn: () => postJson<Person>(`${peoplePath(sheet)}/${p.id}/statement${by}`, {}),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
    ]),
  });
  return (
    <>
      <span data-testid="person-statement-count">{p.statementCount}</span>{' '}
      Last <span data-testid="person-stated-total">{p.statementCount > 0 ? p.statedTotal ?? NA : ''}</span>{' '}
      on <span data-testid="person-statement-date">{p.statementDate ?? ''}</span>{' '}
      <button type="button" data-testid="person-statement" disabled={take.isPending}
        onClick={() => { take.reset(); take.mutate(); }}>
        Statement
      </button>
      {take.isError && (
        <span role="alert" data-testid="person-error">
          {' '}
          {(take.error instanceof HttpError && take.error.reason) || 'The statement could not be taken.'}
        </span>
      )}
    </>
  );
}

function PersonForm({ sheet, editing, onDone }: { sheet: string; editing?: Person; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  // The version of the person the editor loaded; the server refuses the save (409) when someone
  // else has saved the person since.
  const [version, setVersion] = useState(editing?.version);
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const submitOnce = useSingleSubmit();
  const save = useMutation({
    mutationFn: (body: { name: string; grade: string; version?: number }) =>
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
    onError: (error) => {
      if (!(editing && error instanceof HttpError && error.status === 409)) return;
      // Someone else saved the person: re-read the sheet and show the current values in the form.
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }).then(() => {
        const current = queryClient.getQueryData<Person[]>(peopleKey(sheet))?.find((p) => p.id === editing.id);
        if (!current) return;
        setName(current.name);
        setGrade(current.grade);
        setVersion(current.version);
      });
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          // A blank or already-used name is refused by the server (400), which shows the form's error.
          return save.mutateAsync({ name, grade, version });
        });
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
          {(save.error instanceof HttpError && (save.error.status === 400 || save.error.status === 409) && save.error.reason)
            || 'The person could not be saved.'}
        </span>
      )}
    </form>
  );
}

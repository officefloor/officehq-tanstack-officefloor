import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { PersonClaims } from '../claims/PersonClaims';
import { claimsKey, listClaims, sumAmounts } from '../claims/api';
import { dueOf, listRates, rateByGrade, ratesKey, showDue } from '../rates/api';
import { GRADES, addPerson, listPeople, peopleKey, updatePerson, type Person, type PersonInput } from './api';

const EMPTY: PersonInput = { name: '', grade: 'A' };

/** The people on the sheet: a form that adds a person or edits one, and the list in added order,
 *  each with their claimed total, their due-total and their claims. */
export function People({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data);
  const [form, setForm] = useState<PersonInput>(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);

  const save = useMutation({
    mutationFn: (v: { id: number | null; input: PersonInput }) =>
      v.id === null ? addPerson(sheetId, user, v.input) : updatePerson(sheetId, user, v.id, v.input),
    onSuccess: () => {
      setForm(EMPTY);
      setEditing(null);
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  const change = (patch: Partial<PersonInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const open = (p: Person) => {
    setForm({ name: p.name, grade: p.grade });
    setEditing(p.id);
    save.reset();
  };

  const cancel = () => {
    setForm(EMPTY);
    setEditing(null);
    save.reset();
  };

  return (
    <section>
      <h3>People</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ id: editing, input: form });
        }}
      >
        <label>
          Name{' '}
          <input data-testid="person-form-name" value={form.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          Grade{' '}
          <select data-testid="person-form-grade" value={form.grade} onChange={(e) => change({ grade: e.target.value })}>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add person' : 'Save person'}
        </button>
        {editing !== null && (
          <button type="button" data-testid="person-form-cancel" onClick={cancel}>
            Cancel
          </button>
        )}
        {save.isError && <span data-testid="person-form-error">A person needs a name not already in use.</span>}
      </form>
      {people.isError && <p data-testid="people-error">The people could not be loaded.</p>}
      <table>
        <tbody>
          {(people.data ?? []).map((p) => {
            const own = (claims.data ?? []).filter((c) => c.personId === p.id);
            const claimed = sumAmounts(own);
            const pct = byGrade.get(p.grade);
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td data-testid="person-name">{p.name}</td>
                <td data-testid="person-grade">{p.grade}</td>
                <td>
                  Claimed <span data-testid="person-claimed">{claimed}</span>
                </td>
                <td>
                  Due <span data-testid="person-due-total">{showDue(dueOf(claimed, pct))}</span>
                </td>
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                    Edit
                  </button>
                </td>
                <td>
                  <PersonClaims sheetId={sheetId} user={user} personId={p.id} claims={own} pct={pct} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

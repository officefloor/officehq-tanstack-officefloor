import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { PersonClaims } from '../claims/PersonClaims';
import { claimsKey, listClaims, sumAmounts } from '../claims/api';
import { listRates, personDue, rateByGrade, ratesKey, showDue } from '../rates/api';
import { useSheetToday } from '../sheet/api';
import { GRADES, addPerson, listPeople, peopleKey, takeStatement, updatePerson, type Person, type PersonInput } from './api';

const EMPTY: PersonInput = { name: '', grade: 'A' };

/** The people on the sheet: a form that adds a person or edits one, and the list in added order,
 *  each with their claimed total, their due-total (the sum of their claims' due figures; n/a
 *  when their grade has no rate on the sheet's today and no settled or stated claim keeps a figure),
 *  their statements (a button that takes one, the count, the last one's due-total and date) and their claims. */
export function People({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const byGrade = rateByGrade(rates.data, useSheetToday(sheetId, user));
  const [form, setForm] = useState<PersonInput>(EMPTY);
  // The person being edited and the version of them the form loaded.
  const [editing, setEditing] = useState<{ id: number; version: number } | null>(null);

  const save = useMutation({
    mutationFn: async (v: { editing: { id: number; version: number } | null; input: PersonInput }) => {
      if (v.editing === null) return addPerson(sheetId, user, v.input);
      try {
        return await updatePerson(sheetId, user, v.editing.id, v.editing.version, v.input);
      } catch (e) {
        // Someone else saved the person since the form loaded them: show what is stored now. The
        // form keeps the version it loaded, so a save goes through once the editor is opened again.
        if (e instanceof HttpError && e.status === 409) {
          const now = await queryClient.fetchQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId), staleTime: 0 });
          const current = now.find((p) => p.id === v.editing?.id);
          if (current) setForm({ name: current.name, grade: current.grade });
        }
        throw e;
      }
    },
    onSuccess: () => {
      setForm(EMPTY);
      setEditing(null);
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();
  const stateOnce = useSubmitOnce();

  // The person whose last statement was refused; cleared by the next statement taken here.
  const statement = useMutation({
    mutationFn: (id: number) => takeStatement(sheetId, user, id),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    ]),
  });

  const state = (id: number) => {
    stateOnce(() => {
      statement.reset();
      return statement.mutateAsync(id);
    });
  };

  const change = (patch: Partial<PersonInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const open = (p: Person) => {
    setForm({ name: p.name, grade: p.grade });
    setEditing({ id: p.id, version: p.version });
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
          const v = { editing, input: form };
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(v);
          });
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
        {save.isError && (
          <span data-testid="person-form-error">
            {save.error instanceof HttpError && save.error.status === 409
              ? 'Someone else saved this person meanwhile: the form shows what is stored now. Open the editor again to edit it.'
              : 'A person needs a name not already in use.'}
          </span>
        )}
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
                  Due <span data-testid="person-due-total">{showDue(personDue(own, rates.data, p.grade, pct))}</span>
                </td>
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                    Edit
                  </button>
                </td>
                <td>
                  <button type="button" data-testid="person-statement" disabled={statement.isPending}
                    onClick={() => state(p.id)}>
                    Statement
                  </button>{' '}
                  Statements <span data-testid="person-statement-count">{p.statementCount}</span>{' '}
                  last <span data-testid="person-stated-total">{p.statedTotal ?? ''}</span>{' '}
                  on <span data-testid="person-statement-date">{p.statementDate ?? ''}</span>
                  {statement.isError && statement.variables === p.id && (
                    <span data-testid="person-error">The statement could not be taken.</span>
                  )}
                </td>
                <td>
                  <PersonClaims sheetId={sheetId} user={user} personId={p.id} claims={own} grade={p.grade} rates={rates.data} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

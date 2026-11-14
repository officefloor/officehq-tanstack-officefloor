import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { PersonClaims } from '../claims/PersonClaims';
import { claimsKey, listClaims, liveClaims, sumAmounts } from '../claims/api';
import { claimDue, listRates, overdue, personDue, rateByGrade, ratesKey, showDue, sumDues } from '../rates/api';
import { useFloor, useLate, useTerms } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { QuickAdd } from './QuickAdd';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { GRADES, addPatron, addPerson, holdPerson, listPeople, peopleKey, retirePerson, takeStatement, updatePerson, type Person, type PersonInput } from './api';

const EMPTY: PersonInput = { name: '', grade: 'A' };

/** The people on the sheet: a form that adds a person or edits one, and the list in added order,
 *  each with their claimed total, their due-total (the sum of their claims' due figures; n/a
 *  when their grade has no rate on the sheet's today and no settled or stated claim keeps a figure),
 *  their statements (a button that takes one, the count, the last one's due-total and date), whether
 *  they are retired and from when (with a date and a button that retires them), whether they are on
 *  hold (with a button that holds or releases them; held, their OPEN claims count in no total), their
 *  patrons (names sorted, with a picker that names one more) and backing (their patrons' due-totals
 *  added up, skipping n/a), and
 *  their overdue claims (OPEN ones past their due-by; the count and their due added up) and their claims.
 *  Retired people are left out of the list unless "show retired" is on; their claims still count. */
export function People({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const terms = useTerms(sheetId);
  const today = useSheetToday(sheetId, user);
  const charges = useLate(sheetId, today);
  const byGrade = rateByGrade(rates.data, today);
  const [form, setForm] = useState<PersonInput>(EMPTY);
  // The person being edited and the version of them the form loaded.
  const [editing, setEditing] = useState<{ id: number; version: number } | null>(null);
  const [showRetired] = useShowRetired();
  // The retirement date typed on each person's row (blank: the sheet's today).
  const [retireDates, setRetireDates] = useState<Record<number, string>>({});
  // The patron picked on each person's row (blank: none picked).
  const [patronPicks, setPatronPicks] = useState<Record<number, string>>({});

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

  // The person whose last retirement was refused; cleared by the next action on the row.
  const retirement = useMutation({
    mutationFn: (v: { id: number; from: string }) => retirePerson(sheetId, user, v.id, v.from),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
  });

  // The person whose last hold or release was refused; cleared by the next action on the row.
  const holding = useMutation({
    mutationFn: (id: number) => holdPerson(sheetId, user, id),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    ]),
  });

  // The person whose last patron added was refused; cleared by the next action on the row.
  const patronage = useMutation({
    mutationFn: (v: { id: number; patronId: number }) => addPatron(sheetId, user, v.id, v.patronId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
  });

  const resetRowActions = () => {
    statement.reset();
    retirement.reset();
    holding.reset();
    patronage.reset();
  };

  const state = (id: number) => {
    stateOnce(() => {
      resetRowActions();
      return statement.mutateAsync(id);
    });
  };

  const retire = (id: number) => {
    const from = retireDates[id] ?? '';
    stateOnce(() => {
      resetRowActions();
      return retirement.mutateAsync({ id, from });
    });
  };

  const hold = (id: number) => {
    stateOnce(() => {
      resetRowActions();
      return holding.mutateAsync(id);
    });
  };

  const patronize = (id: number) => {
    const patronId = Number(patronPicks[id] ?? '');
    stateOnce(() => {
      resetRowActions();
      return patronage.mutateAsync({ id, patronId });
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

  // Everyone's due-total as their row shows it (null: n/a), and their names, by id.
  const everyone = people.data ?? [];
  const dueOf = new Map(everyone.map((q) => [q.id, personDue(
    liveClaims((claims.data ?? []).filter((c) => c.personId === q.id), [q]),
    rates.data, q.grade, byGrade.get(q.grade), floor, charges)]));
  const nameOf = new Map(everyone.map((q) => [q.id, q.name]));

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
      <QuickAdd sheetId={sheetId} user={user} />
      <ShowRetired />
      {people.isError && <p data-testid="people-error">The people could not be loaded.</p>}
      <table>
        <tbody>
          {(people.data ?? []).filter((p) => showRetired || !p.retired).map((p) => {
            const own = (claims.data ?? []).filter((c) => c.personId === p.id);
            const live = liveClaims(own, [p]);
            const claimed = sumAmounts(live);
            const pct = byGrade.get(p.grade);
            const late = overdue(live, terms, today, (c) => claimDue(c, rates.data, p.grade, floor, charges));
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td data-testid="person-name">{p.name}</td>
                <td data-testid="person-grade">{p.grade}</td>
                <td>
                  Claimed <span data-testid="person-claimed">{claimed}</span>
                </td>
                <td>
                  Due <span data-testid="person-due-total">{showDue(personDue(live, rates.data, p.grade, pct, floor, charges))}</span>
                </td>
                <td>
                  Overdue <span data-testid="person-overdue-count">{late.count}</span>{' '}
                  totalling <span data-testid="person-overdue-total">{late.total}</span>
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
                  {retirement.isError && retirement.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be retired: the date must be a date.</span>
                  )}
                  {holding.isError && holding.variables === p.id && (
                    <span data-testid="person-error">The person could not be held or released.</span>
                  )}
                </td>
                <td>
                  Retired <span data-testid="person-retired">{p.retired ? 'yes' : 'no'}</span>{' '}
                  from <span data-testid="person-retired-from">{p.retiredFrom ?? ''}</span>{' '}
                  <input type="date" data-testid="person-retire-date" value={retireDates[p.id] ?? ''}
                    onChange={(e) => {
                      setRetireDates((d) => ({ ...d, [p.id]: e.target.value }));
                      retirement.reset();
                    }} />{' '}
                  <button type="button" data-testid="person-retire" disabled={retirement.isPending}
                    onClick={() => retire(p.id)}>
                    Retire
                  </button>
                </td>
                <td>
                  On hold <span data-testid="person-hold">{p.held ? 'yes' : 'no'}</span>{' '}
                  <button type="button" data-testid="person-hold-toggle" disabled={holding.isPending}
                    onClick={() => hold(p.id)}>
                    {p.held ? 'Release' : 'Hold'}
                  </button>
                </td>
                <td>
                  Patrons <span data-testid="person-patrons">
                    {p.patronIds.map((id) => nameOf.get(id) ?? '').sort((x, y) => x.localeCompare(y)).join(', ')}
                  </span>{' '}
                  backing <span data-testid="person-backing">{sumDues(p.patronIds.map((id) => dueOf.get(id) ?? null))}</span>{' '}
                  <select data-testid="person-patron-select" value={patronPicks[p.id] ?? ''}
                    onChange={(e) => {
                      setPatronPicks((d) => ({ ...d, [p.id]: e.target.value }));
                      patronage.reset();
                    }}>
                    <option value="">Patron…</option>
                    {everyone.filter((q) => q.id !== p.id).map((q) => (
                      <option key={q.id} value={String(q.id)}>
                        {q.name}
                      </option>
                    ))}
                  </select>{' '}
                  <button type="button" data-testid="person-patron-add" disabled={patronage.isPending}
                    onClick={() => patronize(p.id)}>
                    Add patron
                  </button>
                  {patronage.isError && patronage.variables?.id === p.id && (
                    <span data-testid="person-patron-error">The patron could not be added: not themselves, nor one already named.</span>
                  )}
                </td>
                <td>
                  <PersonClaims sheetId={sheetId} user={user} personId={p.id} claims={own} grade={p.grade} rates={rates.data} floor={floor}
                    terms={terms} today={today} charges={charges} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

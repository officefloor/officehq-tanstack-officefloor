import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { BulkOpen } from '../claims/BulkOpen';
import { PasteClaims } from '../claims/PasteClaims';
import { BulkSettle } from '../claims/BulkSettle';
import { PersonClaims } from '../claims/PersonClaims';
import { claimsKey, listClaims, liveClaims, sumAmounts } from '../claims/api';
import { listPeriods, periodsKey, personPeriodDue } from '../periods/api';
import { claimDue, excessOf, listRates, overdue, personDue, rateByGrade, ratesKey, showDue, sumDues } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { ExportPeople } from './ExportPeople';
import { QuickAdd } from './QuickAdd';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { GRADES, addPatron, addPerson, gradeOn, holdPerson, listPeople, listWatches, peopleKey, retirePerson, takeStatement, updatePerson, watchPerson, watchesKey, type Person, type PersonInput } from './api';

const EMPTY: PersonInput = { name: '', grade: 'A', gradeFrom: '' };

/** The people on the sheet: a form that adds a person or edits one, and the list in added order,
 *  each with their grade in force on the sheet's today, their claimed total, their due-total (the sum of their claims' due figures, at most the sheet's cap, with the excess above it; n/a
 *  when their grade has no rate on the sheet's today and no settled or stated claim keeps a figure),
 *  their statements (a button that takes one, the count, the last one's due-total and date), whether
 *  they are retired and from when (with a date and a button that retires them), whether they have
 *  any hold, their holds (each from a date to an optional date, with dates and a button that adds one;
 *  their OPEN claims dated within a hold, and not bound by a statement, count in no total), their
 *  patrons (names sorted, with a picker that names one more) and backing (their patrons' due-totals
 *  added up, skipping n/a), and
 *  their due-total for each period (see `personPeriodDue`), their overdue claims (OPEN ones past their due-by; the count and their due added up) and their claims, each
 *  with a checkbox that selects it for a button that opens the selected claims together.
 *  Retired people are left out of the list unless "show retired" is on; their claims still count.
 *  Whether the signed-in user watches each person, with a button that starts or stops it.
 *  A button exports everyone, retired people included, as a CSV file. */
export function People({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const watches = useQuery({ queryKey: watchesKey(sheetId, user), queryFn: () => listWatches(sheetId, user) });
  const watched = new Set(watches.data ?? []);
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  const byGrade = rateByGrade(rates.data, today);
  const [form, setForm] = useState<PersonInput>(EMPTY);
  // The person being edited and the version of them the form loaded.
  const [editing, setEditing] = useState<{ id: number; version: number } | null>(null);
  const [showRetired] = useShowRetired();
  // The retirement date typed on each person's row (blank: the sheet's today).
  const [retireDates, setRetireDates] = useState<Record<number, string>>({});
  // The hold dates typed on each person's row (blank from: the sheet's today; blank to: open-ended).
  const [holdDates, setHoldDates] = useState<Record<number, { from: string; to: string }>>({});
  // The patron picked on each person's row (blank: none picked).
  const [patronPicks, setPatronPicks] = useState<Record<number, string>>({});
  // The claims selected for a bulk action.
  const [selected, setSelected] = useState<ReadonlySet<number>>(new Set());
  const select = (id: number, on: boolean) => setSelected((s) => {
    const next = new Set(s);
    if (on) next.add(id); else next.delete(id);
    return next;
  });

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
          if (current) setForm({ name: current.name, grade: current.grade, gradeFrom: '' });
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

  // The person whose last hold was refused; cleared by the next action on the row.
  const holding = useMutation({
    mutationFn: (v: { id: number; from: string; to: string }) => holdPerson(sheetId, user, v.id, v.from, v.to),
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

  // The person whose last watch was refused; cleared by the next action on the row.
  const watching = useMutation({
    mutationFn: (v: { id: number; on: boolean }) => watchPerson(sheetId, user, v.id, v.on),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: watchesKey(sheetId, user) }),
  });

  const resetRowActions = () => {
    statement.reset();
    watching.reset();
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

  const watch = (id: number, on: boolean) => {
    stateOnce(() => {
      resetRowActions();
      return watching.mutateAsync({ id, on });
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
    const { from = '', to = '' } = holdDates[id] ?? {};
    stateOnce(() => {
      resetRowActions();
      return holding.mutateAsync({ id, from, to });
    });
  };

  const changeHold = (id: number, patch: Partial<{ from: string; to: string }>) => {
    setHoldDates((d) => ({ ...d, [id]: { from: '', to: '', ...d[id], ...patch } }));
    holding.reset();
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
    setForm({ name: p.name, grade: p.grade, gradeFrom: '' });
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
    rates.data, (d) => gradeOn(q, d), byGrade.get(q.grade), floor, charges, cap)]));
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
        <label>
          from{' '}
          <input type="date" data-testid="person-form-grade-from" value={form.gradeFrom}
            onChange={(e) => change({ gradeFrom: e.target.value })} />
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
      <PasteClaims sheetId={sheetId} user={user} />
      <ShowRetired />{' '}
      <ExportPeople rows={() => everyone.map((p) => [p.name, gradeOn(p, today), p.retiredFrom ?? '', showDue(dueOf.get(p.id) ?? null)])} />
      <BulkOpen sheetId={sheetId} user={user} selected={[...selected]} onDone={() => setSelected(new Set())} />
      <BulkSettle sheetId={sheetId} user={user} selected={[...selected]} onDone={() => setSelected(new Set())} />
      {people.isError && <p data-testid="people-error">The people could not be loaded.</p>}
      <table>
        <tbody>
          {(people.data ?? []).filter((p) => showRetired || !p.retired).map((p) => {
            const own = (claims.data ?? []).filter((c) => c.personId === p.id);
            const live = liveClaims(own, [p]);
            const claimed = sumAmounts(live);
            const pct = byGrade.get(p.grade);
            const late = overdue(live, today, (c) => claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges));
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td data-testid="person-name">{p.name}</td>
                <td data-testid="person-grade">{p.grade}</td>
                <td>
                  Claimed <span data-testid="person-claimed">{claimed}</span>
                </td>
                <td>
                  Due <span data-testid="person-due-total">{showDue(personDue(live, rates.data, (d) => gradeOn(p, d), pct, floor, charges, cap))}</span>{' '}
                  excess <span data-testid="person-excess">{excessOf(live, rates.data, (d) => gradeOn(p, d), pct, floor, charges, cap)}</span>
                </td>
                {(periods.data ?? []).map((q) => (
                  <td key={q.name}>
                    {q.name} <span data-testid={`person-period-${q.name}-due-total`}>
                      {personPeriodDue(q, p, claims.data ?? [], people.data, rates.data, floor, charges)}</span>
                  </td>
                ))}
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
                  {watching.isError && watching.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be watched.</span>
                  )}
                  {retirement.isError && retirement.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be retired: the date must be a date.</span>
                  )}
                  {holding.isError && holding.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be held: the dates must be dates, the to-date not before the from-date.</span>
                  )}
                </td>
                <td>
                  Watched <span data-testid="person-watched">{watched.has(p.id) ? 'yes' : 'no'}</span>{' '}
                  <button type="button" data-testid="person-watch" disabled={watching.isPending || !watches.isSuccess}
                    onClick={() => watch(p.id, !watched.has(p.id))}>
                    {watched.has(p.id) ? 'Unwatch' : 'Watch'}
                  </button>
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
                  {p.holds.map((h, i) => (
                    <span key={i} data-testid="person-hold-line">{h.from} to {h.to ?? '…'}; </span>
                  ))}
                  <input type="date" data-testid="person-hold-from" value={holdDates[p.id]?.from ?? ''}
                    onChange={(e) => changeHold(p.id, { from: e.target.value })} />{' '}
                  <input type="date" data-testid="person-hold-to" value={holdDates[p.id]?.to ?? ''}
                    onChange={(e) => changeHold(p.id, { to: e.target.value })} />{' '}
                  <button type="button" data-testid="person-hold-toggle" disabled={holding.isPending}
                    onClick={() => hold(p.id)}>
                    Hold
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
                  <PersonClaims sheetId={sheetId} user={user} personId={p.id} claims={own} gradeOn={(d) => gradeOn(p, d)} rates={rates.data} floor={floor}
                    today={today} charges={charges} selected={selected} onSelect={select}
                    people={people.data ?? []} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

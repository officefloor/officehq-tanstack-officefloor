import { Fragment, useState } from 'react';
import { useIsFetching, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { BulkOpen } from '../claims/BulkOpen';
import { PasteClaims } from '../claims/PasteClaims';
import { BulkSettle } from '../claims/BulkSettle';
import { BulkReassign } from '../claims/BulkReassign';
import { BulkRedate } from '../claims/BulkRedate';
import { PersonClaims } from '../claims/PersonClaims';
import { PersonPlans } from '../plans/PersonPlans';
import { backing, claimsKey, patronShares, isActive, correctsAnother, listClaims, liveClaims, showTags, sumAmounts, tagsKey } from '../claims/api';
import { listPeriods, periodOf, periodsKey, personPeriodCarried, personPeriodDue } from '../periods/api';
import { listPools, personCutTotal, poolsKey } from '../pools/api';
import { agedDues, claimDue, excessOf, listRates, overdue, personDue, rateByGrade, ratesKey, showDue, splitDues } from '../rates/api';
import { useCap, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { BulkGrade } from './BulkGrade';
import { BulkRetire } from './BulkRetire';
import { ExportClaims, ExportPeople } from './ExportPeople';
import { ImportPeople } from './ImportPeople';
import { QuickAdd } from './QuickAdd';
import { ColumnChooser, useColumns } from './Columns';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { SortBy, sortPeople, useSortBy } from './SortBy';
import { GRADES, addAlias, addPatron, addPerson, cancelStatement, gradeOn, holdPerson, listPeople, listWatches, mergePerson, movePerson, peopleKey, retirePerson, takeStatement, transferPerson, undoTransfer, unretirePerson, updatePerson, watchPerson, watchesKey, type Person, type PersonInput } from './api';
import { Fig } from '../session/Fig';

const EMPTY: PersonInput = { name: '', grade: 'A', gradeFrom: '', terms: '', termsFrom: '', contact: '', tags: '' };

/** The people on the sheet: a form that adds a person or edits one, and the list in list order (each with buttons that move
 *  them up or down a place; new people go to the end),
 *  each with their grade in force on the sheet's today, their claimed total, their due-total (the sum of their claims' due figures, at most the sheet's cap, with the excess above it, and split into settled and open (see `splitDues`); n/a
 *  when their grade has no rate on the sheet's today and no settled or stated claim keeps a figure),
 *  their statements (a button that takes one, now or as of the date typed beside it, a button that cancels the last, the count, the last one's due-total and date), whether
 *  they are retired and from when (with a date and a button that retires them, a button that unretires them, and their past retirements, each `<from> to <to>`), their snapshot lines (one per SNAPSHOT written for them, oldest first, each `<the day being left> <due-total>`), their statement lines (one per statement still standing, oldest first, each `<statement date> <due-total>`), whether they have
 *  any hold, their holds (each from a date to an optional date, with dates and a button that adds one;
 *  their OPEN claims dated within a hold, and not bound by a statement, count in no total), their
 *  patrons (each `<name> <share>`, sorted by name (see `patronShares`), with a picker that names one more), their plans (see `PersonPlans`), their aliases (sorted as names are
 *  compared, with an entry and a button that adds one) and backing (see `backing`), and
 *  their due-total and carried figure for each period (see `personPeriodDue`, `personPeriodCarried`), the due of their open claims by age (see `agedDues`), their overdue claims (OPEN ones past their due-by; the count and their due added up) and their claims, each
 *  with a checkbox that selects it for a button that opens the selected claims together.
 *  Each person has a checkbox that selects them for a grade given to the selected people together.
 *  Retired people are left out of the list unless "show retired" is on; their claims still count.
 *  Whether each person is active (see `isActive`). Their cut total (see `personCutTotal`).
 *  A picker and a button merge the person into another (see `mergePerson`), and an entry and a
 *  button transfer them to another sheet by id (see `transferPerson`).
 *  Whether the signed-in user watches each person, with a button that starts or stops it.
 *  A button exports everyone, retired people included, as a CSV file, and another exports their claims
 *  (people in list order, each one's claims oldest first, with the figures their rows show).
 *  The list can be sorted on this screen only (see `SortBy`), and each user chooses which of its
 *  columns their screen shows (see `ColumnChooser`): a column switched off has no cell at all. */
export function People({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const watches = useQuery({ queryKey: watchesKey(sheetId, user), queryFn: () => listWatches(sheetId, user) });
  const watched = new Set(watches.data ?? []);
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const pools = useQuery({ queryKey: poolsKey(sheetId), queryFn: () => listPools(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const cap = useCap(sheetId, today);
  const charges = useLate(sheetId, today);
  const byGrade = rateByGrade(rates.data, today);
  const [form, setForm] = useState<PersonInput>(EMPTY);
  // The person being edited and the version of them the form loaded.
  const [editing, setEditing] = useState<{ id: number; version: number } | null>(null);
  const [showRetired] = useShowRetired();
  const [sortBy] = useSortBy();
  const shows = useColumns(sheetId, user);
  // The retirement date typed on each person's row (blank: the sheet's today).
  const [retireDates, setRetireDates] = useState<Record<number, string>>({});
  // The statement date typed on each person's row (blank: the statement is taken now).
  const [asOfDates, setAsOfDates] = useState<Record<number, string>>({});
  // The hold dates typed on each person's row (blank from: the sheet's today; blank to: open-ended).
  const [holdDates, setHoldDates] = useState<Record<number, { from: string; to: string }>>({});
  // The patron picked on each person's row (blank: none picked).
  const [patronPicks, setPatronPicks] = useState<Record<number, string>>({});
  // The person picked on each person's row to merge them into (blank: none picked).
  const [mergePicks, setMergePicks] = useState<Record<number, string>>({});
  // The sheet id typed on each person's row to transfer them to.
  const [transferTos, setTransferTos] = useState<Record<number, string>>({});
  // The grade picked on each person's row (absent: the grade they have).
  const [gradePicks, setGradePicks] = useState<Record<number, string>>({});
  // The alias typed on each person's row.
  const [aliasInputs, setAliasInputs] = useState<Record<number, string>>({});
  // The people selected for a bulk grade.
  const [selectedPeople, setSelectedPeople] = useState<ReadonlySet<number>>(new Set());
  const selectPerson = (id: number, on: boolean) => setSelectedPeople((s) => {
    const next = new Set(s);
    if (on) next.add(id); else next.delete(id);
    return next;
  });
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
          if (current) setForm({ name: current.name, grade: current.grade, gradeFrom: '', terms: current.baseTerms === null ? '' : String(current.baseTerms), termsFrom: '', contact: current.contact ?? '', tags: showTags(current.tags) });
        }
        throw e;
      }
    },
    onSuccess: () => {
      setForm(EMPTY);
      setEditing(null);
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: tagsKey(sheetId) }),
      ]);
    },
    // A refused edit (not one someone else saved meanwhile) closes the editor, its error still
    // showing, and leaves the form ready for an add; the person is as they were.
    onError: (e, v) => {
      if (v.editing !== null && !(e instanceof HttpError && e.status === 409)) {
        setForm(EMPTY);
        setEditing(null);
      }
    },
  });

  const submitOnce = useSubmitOnce();
  const stateOnce = useSubmitOnce();

  // The person whose last statement was refused; cleared by the next statement taken here, or by
  // the statement date typed.
  const statement = useMutation({
    mutationFn: (v: { id: number; asof: string }) => takeStatement(sheetId, user, v.id, v.asof),
    // The date typed goes once the statement is taken or refused, so the next one is taken now unless
    // dated again.
    onError: (_, v) => setAsOfDates((d) => ({ ...d, [v.id]: '' })),
    onSuccess: (_, v) => {
      setAsOfDates((d) => ({ ...d, [v.id]: '' }));
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  // The person whose last statement cancel was refused; cleared by the next action on the row.
  const unstatement = useMutation({
    mutationFn: (id: number) => cancelStatement(sheetId, user, id),
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

  // The person whose last unretirement was refused; cleared by the next action on the row. Their
  // open claims count again, so the claims and pools are read again.
  const unretirement = useMutation({
    mutationFn: (id: number) => unretirePerson(sheetId, user, id),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
    ]),
  });

  // The person whose last hold was refused; cleared by the next action on the row.
  const holding = useMutation({
    mutationFn: (v: { id: number; from: string; to: string }) => holdPerson(sheetId, user, v.id, v.from, v.to),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
    ]),
  });

  // The person whose last patron added was refused; cleared by the next action on the row.
  const patronage = useMutation({
    mutationFn: (v: { id: number; patronId: number }) => addPatron(sheetId, user, v.id, v.patronId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
  });

  // The person whose last alias added was refused; cleared by the next action on the row.
  const aliasing = useMutation({
    mutationFn: (v: { id: number; alias: string }) => addAlias(sheetId, user, v.id, v.alias),
    onSuccess: (_, v) => {
      setAliasInputs((d) => ({ ...d, [v.id]: '' }));
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  // The person whose last merge was refused; cleared by the next action on the row. A merge moves
  // claims, pool members, period totals and watches, so these are read again.
  const merging = useMutation({
    mutationFn: (v: { id: number; intoId: number }) => mergePerson(sheetId, user, v.id, v.intoId),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: watchesKey(sheetId, user) }),
    ]),
  });

  // The person whose last transfer was refused; cleared by the next action on the row. A transfer
  // drops the person's claims, pool memberships, period totals and watches, so these are read again.
  const transferring = useMutation({
    mutationFn: (v: { id: number; to: string }) => transferPerson(sheetId, user, v.id, v.to),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: watchesKey(sheetId, user) }),
    ]),
  });

  // The person whose last transfer undo was refused; cleared by the next action on the row. The person
  // leaves this sheet with their claims, so these are read again.
  const untransferring = useMutation({
    mutationFn: (v: { id: number }) => undoTransfer(sheetId, user, v.id),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: watchesKey(sheetId, user) }),
    ]),
  });

  // The person whose last inline grade was refused; cleared by the next action on the row. Saved as
  // the person form would save the grade from the sheet's today, the rest of the person unchanged.
  const grading = useMutation({
    mutationFn: (v: { id: number; person: Person; grade: string }) => updatePerson(sheetId, user, v.id, v.person.version, {
      name: v.person.name, grade: v.grade, gradeFrom: '', terms: v.person.baseTerms === null ? '' : String(v.person.baseTerms), termsFrom: '',
      contact: v.person.contact ?? '', tags: showTags(v.person.tags),
    }),
    onSuccess: (_, v) => {
      setGradePicks((d) => { const { [v.id]: _gone, ...rest } = d; return rest; });
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: tagsKey(sheetId) }),
      ]);
    },
  });

  // The person whose last move was refused; cleared by the next action on the row.
  const moving = useMutation({
    mutationFn: (v: { id: number; direction: 'up' | 'down' }) => movePerson(sheetId, user, v.id, v.direction),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
  });

  // The person whose last watch was refused; cleared by the next action on the row.
  const watching = useMutation({
    mutationFn: (v: { id: number; on: boolean }) => watchPerson(sheetId, user, v.id, v.on),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: watchesKey(sheetId, user) }),
  });

  const resetRowActions = () => {
    statement.reset();
    unstatement.reset();
    watching.reset();
    retirement.reset();
    unretirement.reset();
    holding.reset();
    patronage.reset();
    aliasing.reset();
    merging.reset();
    transferring.reset();
    untransferring.reset();
    moving.reset();
    grading.reset();
  };

  const grade = (p: Person) => {
    const picked = gradePicks[p.id] ?? p.grade;
    stateOnce(() => {
      resetRowActions();
      return grading.mutateAsync({ id: p.id, person: p, grade: picked });
    });
  };

  const move = (id: number, direction: 'up' | 'down') => {
    stateOnce(() => {
      resetRowActions();
      return moving.mutateAsync({ id, direction });
    });
  };

  const state = (id: number) => {
    stateOnce(() => {
      resetRowActions();
      return statement.mutateAsync({ id, asof: asOfDates[id] ?? '' });
    });
  };

  const unstate = (id: number) => {
    stateOnce(() => {
      resetRowActions();
      return unstatement.mutateAsync(id);
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

  const unretire = (id: number) => {
    stateOnce(() => {
      resetRowActions();
      return unretirement.mutateAsync(id);
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

  const alias = (id: number) => {
    const typed = aliasInputs[id] ?? '';
    stateOnce(() => {
      resetRowActions();
      return aliasing.mutateAsync({ id, alias: typed });
    });
  };

  const merge = (id: number) => {
    const intoId = Number(mergePicks[id] ?? '');
    stateOnce(() => {
      resetRowActions();
      return merging.mutateAsync({ id, intoId });
    });
  };

  const transfer = (id: number) => {
    const to = transferTos[id] ?? '';
    stateOnce(() => {
      resetRowActions();
      return transferring.mutateAsync({ id, to });
    });
  };

  const undoTransferOf = (id: number) => {
    stateOnce(() => {
      resetRowActions();
      return untransferring.mutateAsync({ id });
    });
  };

  const change = (patch: Partial<PersonInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const open = (p: Person) => {
    setForm({ name: p.name, grade: p.grade, gradeFrom: '', terms: p.baseTerms === null ? '' : String(p.baseTerms), termsFrom: '', contact: p.contact ?? '', tags: showTags(p.tags) });
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
  // The exports wait for every read in flight: after the as-of date changes the screen re-reads all.
  const reading = useIsFetching() > 0;

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
        <label>
          Terms{' '}
          <input data-testid="person-form-terms" value={form.terms} placeholder="business days"
            onChange={(e) => change({ terms: e.target.value })} />
        </label>{' '}
        <label>
          from{' '}
          <input type="date" data-testid="person-form-terms-from" value={form.termsFrom}
            onChange={(e) => change({ termsFrom: e.target.value })} />
        </label>{' '}
        <label>
          Contact{' '}
          <input data-testid="person-form-contact" value={form.contact}
            onChange={(e) => change({ contact: e.target.value })} />
        </label>{' '}
        <label>
          Tags{' '}
          <input data-testid="person-form-tags" value={form.tags} placeholder="comma separated"
            onChange={(e) => change({ tags: e.target.value })} />
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
              : 'A person needs a name and contact (if any) not already in use, and terms (if any) a whole number of days.'}
          </span>
        )}
      </form>
      <QuickAdd sheetId={sheetId} user={user} />
      <PasteClaims sheetId={sheetId} user={user} />
      <ImportPeople sheetId={sheetId} user={user} />
      <ShowRetired />{' '}
      <SortBy />{' '}
      <ColumnChooser sheetId={sheetId} user={user} />
      <ExportPeople rows={() => everyone.map((p) => [p.name, gradeOn(p, today), p.retiredFrom ?? '', showDue(dueOf.get(p.id) ?? null)])} loading={reading} />
      <ExportClaims rows={() => everyone.flatMap((p) => (claims.data ?? []).filter((c) => c.personId === p.id).reverse().map((c) => [
        p.name, c.number ?? '', c.date, Number(c.amount).toFixed(2),
        showDue(claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges)), c.status,
        periodOf(periods.data, c.dueBy)?.name ?? '']))} loading={reading} />
      <BulkOpen sheetId={sheetId} user={user} selected={[...selected]} onDone={() => setSelected(new Set())} />
      <BulkGrade sheetId={sheetId} user={user} selected={[...selectedPeople]} onDone={() => setSelectedPeople(new Set())} />
      <BulkRetire sheetId={sheetId} user={user} selected={[...selectedPeople]} onDone={() => setSelectedPeople(new Set())} />
      <BulkSettle sheetId={sheetId} user={user} selected={[...selected]} onDone={() => setSelected(new Set())} />
      <BulkReassign sheetId={sheetId} user={user} selected={[...selected]} people={everyone}
        onDone={() => setSelected(new Set())} />
      <BulkRedate sheetId={sheetId} user={user} selected={[...selected]} onDone={() => setSelected(new Set())} />
      {people.isError && <p data-testid="people-error">The people could not be loaded.</p>}
      <table>
        <tbody>
          {sortPeople((people.data ?? []).filter((p) => showRetired || !p.retired), sortBy, (p) => dueOf.get(p.id) ?? null).map((p) => {
            const own = (claims.data ?? []).filter((c) => c.personId === p.id);
            const live = liveClaims(own, [p]);
            const claimed = sumAmounts(live.filter((c) => !correctsAnother(c)));
            const pct = byGrade.get(p.grade);
            const late = overdue(live, charges, (c) => claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges));
            const aged = agedDues(live, today, (c) => claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges));
            const split = splitDues(live, (c) => claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges));
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td>
                  <input type="checkbox" data-testid="person-select" checked={selectedPeople.has(p.id)}
                    onChange={(e) => selectPerson(p.id, e.target.checked)} />
                </td>
                <td>
                  <button type="button" data-testid="person-move-up" disabled={moving.isPending}
                    onClick={() => move(p.id, 'up')}>
                    Up
                  </button>{' '}
                  <button type="button" data-testid="person-move-down" disabled={moving.isPending}
                    onClick={() => move(p.id, 'down')}>
                    Down
                  </button>
                </td>
                <td data-testid="person-name">{p.name}</td>
                <td data-testid="person-grade">{p.grade}</td>
                <td>
                  <select data-testid="person-inline-grade" value={gradePicks[p.id] ?? p.grade}
                    onChange={(e) => {
                      setGradePicks((d) => ({ ...d, [p.id]: e.target.value }));
                      grading.reset();
                    }}>
                    {GRADES.map((g) => <option key={g} value={g}>{g}</option>)}
                  </select>{' '}
                  <button type="button" data-testid="person-inline-grade-save" disabled={grading.isPending}
                    onClick={() => grade(p)}>
                    Save grade
                  </button>
                </td>
                <td>
                  Active <span data-testid="person-active">{isActive(live, today) ? 'yes' : 'no'}</span>
                </td>
                {shows('terms') && (
                  <td>
                    Terms <span data-testid="person-terms">{p.terms ?? ''}</span>
                  </td>
                )}
                {shows('contact') && (
                  <td>
                    Contact <span data-testid="person-contact">{p.contact ?? ''}</span>
                  </td>
                )}
                <td>
                  Tags <span data-testid="person-tags">{showTags(p.tags)}</span>
                </td>
                <td>
                  From <span data-testid="person-origin">{p.origin ?? ''}</span>
                </td>
                {shows('claimed') && (
                  <td>
                    Claimed <span data-testid="person-claimed"><Fig>{claimed}</Fig></span>
                  </td>
                )}
                {(shows('due-total') || shows('excess')) && (
                  <td>
                    {shows('due-total') && (
                      <>Due <span data-testid="person-due-total"><Fig>{showDue(personDue(live, rates.data, (d) => gradeOn(p, d), pct, floor, charges, cap))}</Fig></span>{' '}</>
                    )}
                    {shows('excess') && (
                      <>excess <span data-testid="person-excess"><Fig>{excessOf(live, rates.data, (d) => gradeOn(p, d), pct, floor, charges, cap)}</Fig></span></>
                    )}
                  </td>
                )}
                <td>
                  Cut <span data-testid="person-cut-total"><Fig>{personCutTotal(pools.data ?? [], p.id)}</Fig></span>
                </td>
                <td>
                  Settled <span data-testid="person-settled-total"><Fig>{split.settled}</Fig></span>{' '}
                  open <span data-testid="person-open-total"><Fig>{split.open}</Fig></span>
                </td>
                {(periods.data ?? []).map((q) => (
                  <td key={q.name}>
                    {q.name} <span data-testid={`person-period-${q.name}-due-total`}>
                      <Fig>{personPeriodDue(q, p, claims.data ?? [], people.data, rates.data, floor, charges)}</Fig></span>
                    {' '}carried <span data-testid={`person-period-${q.name}-carried`}>
                      <Fig>{personPeriodCarried(q, periods.data ?? [], p, claims.data ?? [], people.data, rates.data, floor, charges)}</Fig></span>
                  </td>
                ))}
                <td>
                  Aged 0-30 <span data-testid="person-aged-0-30"><Fig>{aged.upTo30}</Fig></span>{' '}
                  31-60 <span data-testid="person-aged-31-60"><Fig>{aged.upTo60}</Fig></span>{' '}
                  61+ <span data-testid="person-aged-61"><Fig>{aged.over60}</Fig></span>
                </td>
                <td>
                  {shows('overdue-count') && <>Overdue <span data-testid="person-overdue-count">{late.count}</span>{' '}</>}
                  {shows('overdue-total') && <>totalling <span data-testid="person-overdue-total"><Fig>{late.total}</Fig></span></>}
                </td>
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => open(p)}>
                    Edit
                  </button>
                </td>
                <td>
                  <input type="date" data-testid="person-statement-asof" value={asOfDates[p.id] ?? ''}
                    onChange={(e) => {
                      setAsOfDates((d) => ({ ...d, [p.id]: e.target.value }));
                      statement.reset();
                    }} />{' '}
                  <button type="button" data-testid="person-statement" disabled={statement.isPending}
                    onClick={() => state(p.id)}>
                    Statement
                  </button>{' '}
                  <button type="button" data-testid="person-unstate" disabled={unstatement.isPending}
                    onClick={() => unstate(p.id)}>
                    Cancel statement
                  </button>{' '}
                  {shows('statement-count') && <>Statements <span data-testid="person-statement-count">{p.statementCount}</span>{' '}</>}
                  {shows('stated-total') && <>last <span data-testid="person-stated-total"><Fig>{p.statedTotal ?? ''}</Fig></span>{' '}</>}
                  {shows('statement-date') && <>on <span data-testid="person-statement-date">{p.statementDate ?? ''}</span></>}
                  {statement.isError && statement.variables?.id === p.id && (
                    <span data-testid="person-error">The statement could not be taken.</span>
                  )}
                  {unstatement.isError && unstatement.variables === p.id && (
                    <span data-testid="person-error">The statement could not be cancelled: only a steward can, and only a statement there is.</span>
                  )}
                  {watching.isError && watching.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be watched.</span>
                  )}
                  {retirement.isError && retirement.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be retired: the date must be a date.</span>
                  )}
                  {unretirement.isError && unretirement.variables === p.id && (
                    <span data-testid="person-error">The person could not be unretired: they are not retired.</span>
                  )}
                  {holding.isError && holding.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be held: the dates must be dates, the to-date not before the from-date.</span>
                  )}
                  {aliasing.isError && aliasing.variables?.id === p.id && (
                    <span data-testid="person-error">The alias could not be added: it needs to be given, and not be a name or alias already in use.</span>
                  )}
                  {merging.isError && merging.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be merged: only a steward can, into someone else, neither of them retired.</span>
                  )}
                  {moving.isError && moving.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be moved: the first cannot move up, nor the last down.</span>
                  )}
                  {grading.isError && grading.variables?.id === p.id && (
                    <span data-testid="person-error">The grade could not be saved: refused as the person form would refuse it, or someone else saved the person meanwhile.</span>
                  )}
                  {transferring.isError && transferring.variables?.id === p.id && (
                    <span data-testid="person-error">The person could not be transferred: only a steward can, not a retired person, to another sheet where their names match at most one person (not retired), and no one else has their contact.</span>
                  )}
                  {untransferring.isError && untransferring.variables?.id === p.id && (
                    <span data-testid="person-error">The transfer could not be undone: only a steward can, for a person a transfer brought here, on the sheet's today they arrived.</span>
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
                  {shows('retired') && <>Retired <span data-testid="person-retired">{p.retired ? 'yes' : 'no'}</span>{' '}</>}
                  from <span data-testid="person-retired-from">{p.retiredFrom ?? ''}</span>{' '}
                  <input type="date" data-testid="person-retire-date" value={retireDates[p.id] ?? ''}
                    onChange={(e) => {
                      setRetireDates((d) => ({ ...d, [p.id]: e.target.value }));
                      retirement.reset();
                    }} />{' '}
                  <button type="button" data-testid="person-retire" disabled={retirement.isPending}
                    onClick={() => retire(p.id)}>
                    Retire
                  </button>{' '}
                  <button type="button" data-testid="person-unretire" disabled={unretirement.isPending}
                    onClick={() => unretire(p.id)}>
                    Unretire
                  </button>{' '}
                  {p.retirements.map((r, i) => (
                    <Fragment key={i}><span data-testid="person-retirement-line">{r.from} to {r.to}</span>; </Fragment>
                  ))}
                  Snapshots{' '}
                  {p.snapshots.map((s, i) => (
                    <Fragment key={i}><span data-testid="person-snapshot-line">{s.day} {s.dueTotal}</span>; </Fragment>
                  ))}
                  Statements{' '}
                  {p.statements.map((s, i) => (
                    <Fragment key={i}><span data-testid="person-statement-line-line">{s.date} {s.dueTotal}</span>; </Fragment>
                  ))}
                </td>
                <td>
                  {shows('hold') && <>On hold <span data-testid="person-hold">{p.held ? 'yes' : 'no'}</span>{' '}</>}
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
                  Clashing <span data-testid="person-clashing">{p.clashing ? 'yes' : 'no'}</span>
                </td>
                <td>
                  {shows('aliases') && <>Aliases <span data-testid="person-aliases">{p.aliases.join(', ')}</span>{' '}</>}
                  <input data-testid="person-alias-input" value={aliasInputs[p.id] ?? ''}
                    onChange={(e) => {
                      setAliasInputs((d) => ({ ...d, [p.id]: e.target.value }));
                      aliasing.reset();
                    }} />{' '}
                  <button type="button" data-testid="person-alias-add" disabled={aliasing.isPending}
                    onClick={() => alias(p.id)}>
                    Add alias
                  </button>
                </td>
                <td>
                  {shows('patrons') && (
                    <>Patrons <span data-testid="person-patrons">
                      {patronShares(p.patronIds).map((s) => ({ name: nameOf.get(s.id) ?? '', share: s.share })).sort((x, y) => x.name.localeCompare(y.name)).map((s) => `${s.name} ${(s.share / 100).toFixed(2)}`).join(', ')}
                    </span>{' '}</>
                  )}
                  {shows('backing') && <>backing <span data-testid="person-backing"><Fig>{backing(claims.data ?? [], p.patronIds, today)}</Fig></span>{' '}</>}
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
                  <select data-testid="person-merge-into" value={mergePicks[p.id] ?? ''}
                    onChange={(e) => {
                      setMergePicks((d) => ({ ...d, [p.id]: e.target.value }));
                      merging.reset();
                    }}>
                    <option value="">Merge into…</option>
                    {everyone.map((q) => (
                      <option key={q.id} value={String(q.id)}>
                        {q.name}
                      </option>
                    ))}
                  </select>{' '}
                  <button type="button" data-testid="person-merge" disabled={merging.isPending}
                    onClick={() => merge(p.id)}>
                    Merge
                  </button>{' '}
                  <input data-testid="person-transfer-to" placeholder="Sheet id" value={transferTos[p.id] ?? ''}
                    onChange={(e) => {
                      setTransferTos((d) => ({ ...d, [p.id]: e.target.value }));
                      transferring.reset();
                    }} />{' '}
                  <button type="button" data-testid="person-transfer" disabled={transferring.isPending}
                    onClick={() => transfer(p.id)}>
                    Transfer
                  </button>{' '}
                  <button type="button" data-testid="person-transfer-undo" disabled={untransferring.isPending}
                    onClick={() => undoTransferOf(p.id)}>
                    Undo transfer
                  </button>
                </td>
                <td>
                  <PersonPlans sheetId={sheetId} user={user} personId={p.id} />
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

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';
import { asFlag, asNumber, useSearchParam } from '../../url/useSearchParam';
import { useCurrentUser } from '../session/SignIn';
import { BulkOpen, ClaimedTotal, PersonClaims, claimDue, claimed, claimsKey, counts, isOverdue, useClaims, useLate } from '../claims/Claims';
import { NA, rateOn, total, useRates } from '../rates/Rates';
import { useSheetToday } from '../sheet/SheetToday';
import { useFloor } from '../settings/Settings';

// The people on the sheet, in the order they were added. One form adds a person, or edits the
// person whose editor is open (URL state `editPerson`, the person's id).
// A statement taken for a person records their due-total and binds their counting claims' figures.
// A retired person's row is left out unless "show retired" (URL state `showRetired`) is on; their
// claims keep counting in every total. A person on hold keeps their SETTLED claims in every total,
// but their OPEN claims count in none.
// A person may name other people on the sheet as their patrons; their backing is their patrons'
// due-totals added up, skipping n/a (0.00 with no patrons).
export type Person = { id: number; name: string;
  /** The grade in force on the sheet's today. */
  grade: string;
  /** Every grade the person was given, each in force from its from-date (null: from the beginning of time). */
  grades: DatedGrade[];
  version: number;
  /** How many statements have been taken for the person. */
  statementCount: number;
  /** The last statement's due-total; null when none was taken, or it was n/a. */
  statedTotal: string | null;
  /** The sheet's today when the last statement was taken; null when none was. */
  statementDate: string | null;
  /** Whether the person is retired; no claim of theirs may be dated after `retiredFrom`. */
  retired: boolean;
  /** The date the person is retired from; null when they are not retired. */
  retiredFrom: string | null;
  /** Whether the person is on hold: their OPEN claims count in no total. */
  held: boolean;
  /** The names of the other people the person has named as their patrons, sorted. */
  patrons: string[] };

export type DatedGrade = { grade: string; from: string | null };

/** The person's grade in force on the date (yyyy-MM-dd): the one with the latest from-date on or
 *  before it. The grade in force on the sheet's today when the date is not known yet. */
export function gradeOn(person: Person, date: string | undefined): string {
  if (date === undefined) return person.grade;
  let grade = person.grades[0]?.grade ?? person.grade;
  for (const g of person.grades) {
    if (g.from === null || g.from <= date) grade = g.grade;
  }
  return grade;
}

export const GRADES = ['A', 'B', 'C'];

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

function useShowRetired() {
  return useSearchParam('showRetired', asFlag);
}

/** The "show retired" toggle: off when the screen is first opened, left as it is by loading a sheet. */
function ShowRetired() {
  const [show, setShow] = useShowRetired();
  return (
    <label>
      <input type="checkbox" data-testid="show-retired" checked={show}
        onChange={(e) => setShow(e.target.checked || undefined)} />{' '}
      Show retired
    </label>
  );
}

/** A person's due-total is the sum of their counted claims' already-rounded figures (0.00 with none).
 *  It is n/a when the grade in force on the sheet's today has no rate there, unless a counted claim has
 *  a figure (settled, bound by a statement, or at the grade in force on its date): then it is the sum
 *  of the figures there are. */
export function useDueTotalOf(sheet: string): (p: Person) => string {
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  return (p: Person) => {
    const dues = claims.filter((c) => c.personId === p.id && counts(c, p.held))
      .map((c) => claimDue(c, rateOn(rates, gradeOn(p, c.date), c.date), floor, late));
    if (rateOn(rates, gradeOn(p, today), today) === undefined && dues.every((d) => d === NA)) return NA;
    return total(dues);
  };
}

/** A CSV field: wrapped in double quotes, with any quote inside doubled, when it holds a comma,
 *  a quote or a line break. */
function csvField(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** Every person in list order, retired people included: their name, the grade in force on the
 *  sheet's today, the retired-from date (blank when not retired) and the due-total as shown. */
function peopleCsv(people: Person[], today: string | undefined, dueTotalOf: (p: Person) => string): string {
  const lines = [['name', 'grade', 'retired-from', 'due-total'],
    ...people.map((p) => [p.name, gradeOn(p, today), p.retiredFrom ?? '', dueTotalOf(p)])];
  return lines.map((fields) => fields.map(csvField).join(',') + '\n').join('');
}

/** Hands the text to the browser as a downloaded file. */
function download(fileName: string, text: string) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function People({ sheet }: { sheet: string }) {
  const people = usePeople(sheet);
  const [showRetired] = useShowRetired();
  const listed = people.filter((p) => showRetired || !p.retired);
  const [editId, setEditId] = useEditPerson();
  // Counts each press of an Edit button, so opening the editor again reloads it with the current values.
  const [opened, setOpened] = useState(0);
  const editing = people.find((p) => p.id === editId);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const { terms } = late;
  const claimsOf = (p: Person) => claims.filter((c) => c.personId === p.id);
  const isHeld = (personId: number) => people.some((p) => p.id === personId && p.held);
  const dueTotalOf = useDueTotalOf(sheet);
  // A person's overdue claims are their counted claims that are overdue; the overdue-total adds up
  // their figures, skipping n/a.
  const overdueOf = (p: Person) =>
    claimsOf(p).filter((c) => counts(c, p.held) && isOverdue(c, terms, today));
  const backingOf = (p: Person) =>
    total(people.filter((o) => p.patrons.includes(o.name)).map(dueTotalOf));
  const overdueTotalOf = (p: Person) =>
    total(overdueOf(p).map((c) => claimDue(c, rateOn(rates, gradeOn(p, c.date), c.date), floor, late)));

  return (
    <section>
      <h2>People</h2>
      {/* Keyed so opening a person's editor (again) or closing it refills the form. */}
      <PersonForm key={editing ? `edit-${editing.id}-${opened}` : 'add'} sheet={sheet} editing={editing}
        onDone={() => setEditId(undefined)} />
      <QuickAdd sheet={sheet} />
      <ShowRetired />{' '}
      <button type="button" data-testid="export-people"
        onClick={() => download('people.csv', peopleCsv(people, today, dueTotalOf))}>
        Export CSV
      </button>
      <BulkOpen sheet={sheet} />
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Claimed</th>
            <th>Due</th>
            <th>Overdue</th>
            <th>Overdue total</th>
            <th>Statements</th>
            <th>Retired</th>
            <th>Hold</th>
            <th>Patrons</th>
            <th>Backing</th>
            <th>Claims</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {listed.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{gradeOn(p, today)}</td>
              <td data-testid="person-claimed">{claimed(claimsOf(p), isHeld)}</td>
              <td data-testid="person-due-total">{dueTotalOf(p)}</td>
              <td data-testid="person-overdue-count">{overdueOf(p).length}</td>
              <td data-testid="person-overdue-total">{overdueTotalOf(p)}</td>
              <PersonActions sheet={sheet} person={p} people={people} />
              <td data-testid="person-backing">{backingOf(p)}</td>
              <td>
                <PersonClaims sheet={sheet} personId={p.id} claims={claimsOf(p)}
                  pctOn={(date) => rateOn(rates, gradeOn(p, date), date)} />
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
      <ClaimedTotal sheet={sheet} held={isHeld} />
      <p>
        Due total: <strong data-testid="rollup-due-total">{total(people.map(dueTotalOf))}</strong>
      </p>
      <p>
        Overdue: <strong data-testid="rollup-overdue-count">
          {people.reduce((n, p) => n + overdueOf(p).length, 0)}</strong>{' '}
        Overdue total: <strong data-testid="rollup-overdue-total">{total(people.map(overdueTotalOf))}</strong>
      </p>
    </section>
  );
}

/** The person's statements (how many, the last one's due-total and date, and the button that takes
 *  one) and their retirement (yes/no, the date, and the control that retires them from a date; blank
 *  is the sheet's today), and their hold (yes/no, and the button that puts them on hold or takes
 *  them off it), and their patrons (the names, and the control that names another person on the sheet
 *  as one: the person themselves and those already named are not offered). A refused action shows
 *  the row's error, which the row's next action clears. */
function PersonActions({ sheet, person: p, people }: { sheet: string; person: Person; people: Person[] }) {
  const queryClient = useQueryClient();
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const [retireFrom, setRetireFrom] = useState('');
  const [patronId, setPatronId] = useState('');
  const offered = people.filter((o) => o.id !== p.id && !p.patrons.includes(o.name));
  const refetch = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
    queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  ]);
  const take = useMutation({
    mutationFn: () => postJson<Person>(`${peoplePath(sheet)}/${p.id}/statement${by}`, {}),
    onSuccess: refetch,
  });
  const retire = useMutation({
    mutationFn: (from: string) => postJson<Person>(`${peoplePath(sheet)}/${p.id}/retire${by}`, { from }),
    onSuccess: async () => {
      await refetch();
      setRetireFrom('');
    },
  });
  const hold = useMutation({
    mutationFn: () => postJson<Person>(`${peoplePath(sheet)}/${p.id}/hold${by}`, {}),
    onSuccess: refetch,
  });
  const patron = useMutation({
    mutationFn: (id: number | null) => postJson<Person>(`${peoplePath(sheet)}/${p.id}/patrons${by}`, { patronId: id }),
    onSuccess: async () => {
      await refetch();
      setPatronId('');
    },
  });
  const reset = () => {
    take.reset();
    retire.reset();
    hold.reset();
    patron.reset();
  };
  const failed = take.isError ? take : retire.isError ? retire : hold.isError ? hold : undefined;
  return (
    <>
      <td>
        <span data-testid="person-statement-count">{p.statementCount}</span>{' '}
        Last <span data-testid="person-stated-total">{p.statementCount > 0 ? p.statedTotal ?? NA : ''}</span>{' '}
        on <span data-testid="person-statement-date">{p.statementDate ?? ''}</span>{' '}
        <button type="button" data-testid="person-statement" disabled={take.isPending}
          onClick={() => { reset(); take.mutate(); }}>
          Statement
        </button>
      </td>
      <td>
        <span data-testid="person-retired">{p.retired ? 'yes' : 'no'}</span>{' '}
        from <span data-testid="person-retired-from">{p.retiredFrom ?? ''}</span>{' '}
        <input type="date" data-testid="person-retire-date" value={retireFrom}
          onChange={(e) => { reset(); setRetireFrom(e.target.value); }} />{' '}
        <button type="button" data-testid="person-retire" disabled={retire.isPending}
          onClick={() => { reset(); retire.mutate(retireFrom); }}>
          Retire
        </button>
        {failed && (
          <span role="alert" data-testid="person-error">
            {' '}
            {(failed.error instanceof HttpError && failed.error.reason)
              || (failed === take ? 'The statement could not be taken.'
                : failed === hold ? 'The hold could not be changed.' : 'The person could not be retired.')}
          </span>
        )}
      </td>
      <td>
        <span data-testid="person-hold">{p.held ? 'yes' : 'no'}</span>{' '}
        <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending}
          onClick={() => { reset(); hold.mutate(); }}>
          {p.held ? 'Release' : 'Hold'}
        </button>
      </td>
      <td>
        <span data-testid="person-patrons">{p.patrons.join(', ')}</span>{' '}
        <select data-testid="person-patron-select" value={patronId}
          onChange={(e) => { reset(); setPatronId(e.target.value); }}>
          <option value="">Choose…</option>
          {offered.map((o) => (
            <option key={o.id} value={String(o.id)}>{o.name}</option>
          ))}
        </select>{' '}
        <button type="button" data-testid="person-patron-add" disabled={patron.isPending}
          onClick={() => { reset(); patron.mutate(patronId ? Number(patronId) : null); }}>
          Add patron
        </button>
        {patron.isError && (
          <span role="alert" data-testid="person-patron-error">
            {' '}
            {(patron.error instanceof HttpError && patron.error.reason) || 'The patron could not be added.'}
          </span>
        )}
      </td>
    </>
  );
}

/** The quick-add bar: a DRAFT claim of this amount, dated the sheet's today with an empty note, for
 *  the named person, who is added with grade A when not on the sheet. The server refuses (400) what
 *  the person or claim form would refuse, and then adds no one; the bar shows its error. */
function QuickAdd({ sheet }: { sheet: string }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const submitOnce = useSingleSubmit();
  const add = useMutation({
    mutationFn: (body: { name: string; amount: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheet)}/quick-add${by}`, body),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
      setName('');
      setAmount('');
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          add.reset();
          return add.mutateAsync({ name, amount });
        });
      }}
    >
      <label>
        Quick add{' '}
        <input
          data-testid="quick-name"
          placeholder="Name"
          value={name}
          onChange={(e) => {
            add.reset();
            setName(e.target.value);
          }}
        />
      </label>{' '}
      <input
        data-testid="quick-amount"
        placeholder="Amount"
        value={amount}
        onChange={(e) => {
          add.reset();
          setAmount(e.target.value);
        }}
      />{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add claim
      </button>
      {add.isError && (
        <span role="alert" data-testid="quick-error">
          {' '}
          {(add.error instanceof HttpError && add.error.status === 400 && add.error.reason)
            || 'The claim could not be added.'}
        </span>
      )}
    </form>
  );
}

function PersonForm({ sheet, editing, onDone }: { sheet: string; editing?: Person; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  // The date a save records the grade from: the sheet's today until one is entered. The first grade
  // a person is given applies from the beginning of time, whatever this says.
  const today = useSheetToday(sheet);
  const [gradeFrom, setGradeFrom] = useState<string | undefined>(undefined);
  const shownGradeFrom = gradeFrom ?? today ?? '';
  // The version of the person the editor loaded; the server refuses the save (409) when someone
  // else has saved the person since.
  const [version, setVersion] = useState(editing?.version);
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const submitOnce = useSingleSubmit();
  const save = useMutation({
    mutationFn: (body: { name: string; grade: string; gradeFrom: string; version?: number }) =>
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
        setGradeFrom(undefined);
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
          // A grade from-date that is not a date is refused by the server (400); a blank one is the sheet's today.
          return save.mutateAsync({ name, grade, gradeFrom: shownGradeFrom.trim(), version });
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
      <label>
        Grade from{' '}
        <input
          type="date"
          data-testid="person-form-grade-from"
          value={shownGradeFrom}
          onChange={(e) => {
            save.reset();
            setGradeFrom(e.target.value);
          }}
        />
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

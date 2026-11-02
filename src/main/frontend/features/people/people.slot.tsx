import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonCells, PersonColumnHead, PersonDetail } from '../../slots/defs/personRow';
import { claimsKey, type Claim } from '../claims/useClaims';
import { useShowRetired } from '../retire/useShowRetired';
import { GRADES, fetchPeople, peopleKey, usePeople, type Person } from './usePeople';

/** The sheet's people: a form that adds a person, or edits one when their editor is open, and the
 *  list of everyone in the order they were added (retired people only when "show retired" is on), each
 *  with the grade in force on the sheet's today. An edit records the form's grade from its from-date
 *  (blank: the sheet's today); the first grade a person is given applies from the beginning of time.
 *  The editor remembers the version of the person it loaded; when someone else has saved them since,
 *  the save is refused (409) and the editor shows their current values, at their current version. Each person is a <tbody> so features can add
 *  cells and rows of their own to it (slots/defs/personRow). A quick-add bar takes a name and an amount
 *  and adds a draft claim for that person (adding them with grade A when the name is new) in one change. */
function People({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const [showRetired] = useShowRetired();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const [gradeFrom, setGradeFrom] = useState('');
  const [version, setVersion] = useState<number | null>(null);

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { id: number | null; name: string; grade: string; gradeFrom: string; version: number | null }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
      return f.id === null
        ? postJson<Person>(base, { name: f.name, grade: f.grade })
        : postJson<Person>(`${base}/${f.id}`, { name: f.name, grade: f.grade, gradeFrom: f.gradeFrom, version: f.version });
    },
    onSuccess: async () => {
      setEditing(null);
      setName('');
      setGrade('A');
      setGradeFrom('');
      setVersion(null);
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: async (err, f) => {
      if (!(err instanceof HttpError && err.status === 409) || f.id === null) return;
      const people = await queryClient.fetchQuery({
        queryKey: peopleKey(sheetId),
        queryFn: () => fetchPeople(sheetId),
        staleTime: 0,
      });
      const current = people.find((p) => p.id === f.id);
      if (current) {
        setName(current.name);
        setGrade(current.grade);
        setVersion(current.version);
      }
    },
  });

  const [quickName, setQuickName] = useState('');
  const [quickAmount, setQuickAmount] = useState('');
  const quickOnce = useSingleFlight();
  const quick = useMutation({
    mutationFn: (f: { name: string; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick`, f),
    onSuccess: async () => {
      setQuickName('');
      setQuickAmount('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  const openEditor = (p: Person) => {
    save.reset();
    setEditing(p.id);
    setName(p.name);
    setGrade(p.grade);
    setGradeFrom('');
    setVersion(p.version);
  };

  const cancel = () => {
    save.reset();
    setEditing(null);
    setName('');
    setGrade('A');
    setGradeFrom('');
    setVersion(null);
  };

  return (
    <section data-testid="people">
      <form
        data-testid="person-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ id: editing, name, grade, gradeFrom, version });
          });
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
        <label>
          from{' '}
          <input
            type="date"
            data-testid="person-form-grade-from"
            value={gradeFrom}
            onChange={(e) => {
              setGradeFrom(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add person' : 'Save person'}
        </button>
        {editing !== null && (
          <>
            {' '}
            <button type="button" data-testid="person-form-cancel" onClick={cancel}>
              Cancel
            </button>
          </>
        )}
        {save.isError && <p data-testid="person-form-error">{save.error.message}</p>}
      </form>
      <form
        data-testid="quick-add"
        onSubmit={(e) => {
          e.preventDefault();
          quickOnce(() => {
            quick.reset();
            return quick.mutateAsync({ name: quickName, amount: quickAmount });
          });
        }}
      >
        <label>
          Quick add: name{' '}
          <input
            data-testid="quick-name"
            value={quickName}
            onChange={(e) => {
              setQuickName(e.target.value);
              quick.reset();
            }}
          />
        </label>{' '}
        <label>
          Amount{' '}
          <input
            data-testid="quick-amount"
            value={quickAmount}
            onChange={(e) => {
              setQuickAmount(e.target.value);
              quick.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="quick-submit" disabled={quick.isPending}>
          Add draft claim
        </button>
        {quick.isError && <p data-testid="quick-error">{quick.error.message}</p>}
      </form>
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PersonColumnHead.Slot sheetId={sheetId} />
            <th />
          </tr>
        </thead>
        {(people.data ?? []).filter((p) => showRetired || !p.retiredFrom).map((p) => (
          <tbody key={p.id} data-testid={`person-row-${p.id}`}>
            <tr>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <PersonCells.Slot sheetId={sheetId} personId={p.id} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => openEditor(p)}>
                  Edit
                </button>
              </td>
            </tr>
            <PersonDetail.Slot sheetId={sheetId} personId={p.id} />
          </tbody>
        ))}
      </table>
      {people.isError && <p data-testid="people-error">{people.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({
  order: 10,
  Component: ({ sheetId }) => <People sheetId={sheetId} />,
});

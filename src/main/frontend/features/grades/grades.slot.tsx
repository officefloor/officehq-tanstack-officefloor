import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { gradesKey, gradesPath, useGrades, type GradeRow } from './grades';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';

type GradeMerged = { merged: number; people: Person[] };

// The sheet's grades, a form that adds one (a code of one to three capital letters not yet a grade; the server refuses
// anything else) and a form that retires one. People on a retired grade keep it, but the server refuses it as a
// choice for a new person or in an edit (the person form, the row's grade, the import); the quick-add bar gives a new
// person grade A when the default grade is retired.
function Grades({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: grades = [] } = useGrades(sheet);
  const [grade, setGrade] = useState('A');

  const retire = useMutation({
    mutationFn: (body: { grade: string }) => postJson<GradeRow[]>(byUser(`${gradesPath(sheet)}/retire`, user), body),
    onSuccess: (updated) => queryClient.setQueryData(gradesKey(sheet), updated),
  });
  const send = useSubmitOnce(retire);

  const [code, setCode] = useState('');
  const add = useMutation({
    mutationFn: (body: { grade: string }) => postJson<GradeRow[]>(byUser(gradesPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(gradesKey(sheet), updated);
      setCode('');
    },
  });
  const sendAdd = useSubmitOnce(add);

  // A steward merges a grade into another: the server gives everyone on it today the other, exactly as the person form
  // would, and refuses anyone else or the same grade twice. Shows how many people moved.
  const [from, setFrom] = useState('A');
  const [into, setInto] = useState('B');
  const merge = useMutation({
    mutationFn: (body: { from: string; into: string }) =>
      postJson<GradeMerged>(byUser(`${gradesPath(sheet)}/merge`, user), body),
    onSuccess: (result) => {
      queryClient.setQueryData(peopleKey(sheet), result.people);
      void queryClient.invalidateQueries();
    },
  });
  const sendMerge = useSubmitOnce(merge);
  const choices = grades.length ? grades.map((g) => g.grade) : ['A', 'B', 'C'];

  const change = (next: string) => {
    setGrade(next);
    retire.reset();
    merge.reset();
    add.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a grade already retired.
    merge.reset();
    add.reset();
    send({ grade });
  };

  return (
    <section>
      <h2>Grades</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          retire.reset();
          merge.reset();
          sendAdd({ grade: code });
        }}
      >
        <label>
          New grade{' '}
          <input
            data-testid="grade-add-code"
            placeholder="1 to 3 capital letters"
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              add.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="grade-add" disabled={add.isPending}>
          Add grade
        </button>
        {add.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}A new grade needs a code of one to three capital letters that is not yet a grade.
          </span>
        )}
      </form>
      <form onSubmit={submit}>
        <label>
          Grade{' '}
          <select data-testid="grade-retire-select" value={grade} onChange={(e) => change(e.target.value)}>
            {(grades.length ? grades.map((g) => g.grade) : ['A', 'B', 'C']).map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {retire.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}That grade cannot be retired (it is already retired).
          </span>
        )}
      </form>
      <p>
        <label>
          Merge{' '}
          <select
            data-testid="grade-merge-from"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              merge.reset();
              add.reset();
            }}
          >
            {choices.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          into{' '}
          <select
            data-testid="grade-merge-into"
            value={into}
            onChange={(e) => {
              setInto(e.target.value);
              merge.reset();
              add.reset();
            }}
          >
            {choices.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button
          type="button"
          data-testid="grade-merge"
          disabled={merge.isPending}
          onClick={() => {
            retire.reset();
            add.reset();
            sendMerge({ from, into });
          }}
        >
          Merge grade
        </button>
        {merge.isSuccess && (
          <>
            {' '}Moved <strong data-testid="grade-merged">{merge.data.merged}</strong>
          </>
        )}
        {merge.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}That merge was refused: only a steward may merge a grade, into a different grade.
          </span>
        )}
      </p>
      <ul>
        {grades.map((g) => (
          <li key={g.grade} data-testid={`grade-row-${g.grade}`}>
            {g.grade} retired: <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 5.2, Component: Grades });

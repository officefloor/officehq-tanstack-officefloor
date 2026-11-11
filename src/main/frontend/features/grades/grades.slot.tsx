import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { useGrades } from './grades';
import type { Grade } from './grades';

// The sheet's grades: A, B and C, then those added. A grade is added by its code, one to three
// capital letters not yet a grade on the sheet; anything else is refused. A select and a retire button, each grade listed with whether it is retired. A
// grade already retired is refused. People on a retired grade keep it, but it cannot be chosen for a new
// person or in an edit, and a default grade that is retired falls back to A. A steward may also merge a
// grade into another: everyone on the merged grade today is given the other from the sheet's today.
function Grades({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const grades = useGrades(sheetId);
  const [grade, setGrade] = useState('A');
  const [error, setError] = useState('');
  const [user] = useUser();

  const [from, setFrom] = useState('A');
  const [into, setInto] = useState('B');
  const [merged, setMerged] = useState('');
  const [code, setCode] = useState('');

  const once = useOnce();
  const retire = useMutation({
    mutationFn: (body: { grade: string }) =>
      postJson<{ grades: Grade[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] }),
    onError: () => setError('That grade could not be retired.'),
  });

  const add = useMutation({
    mutationFn: (body: { grade: string }) =>
      postJson<{ grades: Grade[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`, body, user),
    onSuccess: () => {
      setCode('');
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('That grade could not be added.'),
  });

  const submitAdd = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const grade = code.trim();
    if (!/^[A-Z]{1,3}$/.test(grade)) {
      setError('A grade is one to three capital letters.');
      return;
    }
    if ((grades.data ?? []).some((g) => g.grade === grade)) {
      setError(`Grade ${grade} is already a grade.`);
      return;
    }
    once((done) => add.mutate({ grade }, { onSettled: done }));
  };

  const merge = useMutation({
    mutationFn: (body: { from: string; into: string }) =>
      postJson<{ merged: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/merge`, body, user),
    onSuccess: (data) => {
      setMerged(String(data.merged));
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('Those grades could not be merged.'),
  });

  const submitMerge = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setMerged('');
    if (from === into) {
      setError('A grade cannot be merged into itself.');
      return;
    }
    once((done) => merge.mutate({ from, into }, { onSettled: done }));
  };

  const options = (grades.data ?? []).map((g) => (
    <option key={g.grade} value={g.grade}>
      {g.grade}
    </option>
  ));

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if ((grades.data ?? []).some((g) => g.grade === grade && g.retired)) {
      setError(`Grade ${grade} is already retired.`);
      return;
    }
    once((done) => retire.mutate({ grade }, { onSettled: done }));
  };

  return (
    <section data-testid="grades">
      <h2>Grades</h2>
      <form onSubmit={submitAdd}>
        <label>
          New grade{' '}
          <input
            data-testid="grade-add-code"
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="grade-add" disabled={add.isPending}>
          Add grade
        </button>
      </form>
      <form onSubmit={submit}>
        <label>
          Grade{' '}
          <select
            data-testid="grade-retire-select"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              setError('');
            }}
          >
            {(grades.data ?? []).map((g) => (
              <option key={g.grade} value={g.grade}>
                {g.grade}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {error && (
          <p role="alert" data-testid="grade-error">
            {error}
          </p>
        )}
      </form>
      <form onSubmit={submitMerge}>
        <label>
          Merge{' '}
          <select
            data-testid="grade-merge-from"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              setError('');
            }}
          >
            {options}
          </select>
        </label>{' '}
        <label>
          into{' '}
          <select
            data-testid="grade-merge-into"
            value={into}
            onChange={(e) => {
              setInto(e.target.value);
              setError('');
            }}
          >
            {options}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-merge" disabled={merge.isPending}>
          Merge grade
        </button>{' '}
        People moved: <span data-testid="grade-merged">{merged}</span>
      </form>
      <ul data-testid="grade-list">
        {(grades.data ?? []).map((g) => (
          <li key={g.grade} data-testid={`grade-row-${g.grade}`}>
            {g.grade} retired: <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -6, Component: Grades });

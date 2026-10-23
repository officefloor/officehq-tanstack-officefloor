import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { GRADES } from '../people/people';
import { gradesKey, useGrades } from './grades';
import type { Grade } from './grades';

/** A grade merge done: the grade merged, the grade its people were given and how many were given it. */
type GradeMerge = { from: string; into: string; changed: number };

/** The grades, each with whether it is retired, the select and button a grade is retired with, and the two selects
 *  and button a steward merges a grade into another with (the same grade twice is refused). People on a
 *  retired grade keep it, but it cannot be chosen for a new person or in an edit; retiring one already retired is
 *  refused. */
function Grades({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const grades = useGrades(sheetId).data ?? [];
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const [from, setFrom] = useState<string>(GRADES[0]);
  const [into, setInto] = useState<string>(GRADES[1]);
  const [merged, setMerged] = useState('');
  const mergeOnce = useSubmitOnce();

  const merge = useMutation({
    mutationFn: (body: { from: string; into: string }) =>
      postJson<GradeMerge>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/merge`, { user, ...body }),
    onSuccess: async (r) => {
      setMerged(`${r.from} into ${r.into}: ${r.changed}`);
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : `Could not merge the grade ${from}.`),
    onSettled: mergeOnce.done,
  });

  const submitMerge = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (from === into) {
      setError('Choose two different grades to merge.');
      return;
    }
    if (!mergeOnce.start()) return;
    merge.mutate({ from, into });
  };

  const retire = useMutation({
    mutationFn: (g: string) =>
      postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, { user, grade: g }),
    onSuccess: (saved) => queryClient.setQueryData(gradesKey(sheetId), saved),
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : `Could not retire the grade ${grade}.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (grades.some((g) => g.grade === grade && g.retired)) {
      setError(`The grade ${grade} is already retired.`);
      return;
    }
    if (!once.start()) return;
    retire.mutate(grade);
  };

  return (
    <section data-testid="grades">
      <h2>Grades</h2>
      <form onSubmit={submit}>
        <select
          data-testid="grade-retire-select"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            setError('');
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
      </form>
      <form onSubmit={submitMerge}>
        Merge{' '}
        <select
          data-testid="grade-merge-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        into{' '}
        <select
          data-testid="grade-merge-into"
          value={into}
          onChange={(e) => {
            setInto(e.target.value);
            setError('');
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="grade-merge" disabled={merge.isPending}>
          Merge grade
        </button>{' '}
        <span data-testid="grade-merged">{merged}</span>
      </form>
      {error && (
        <p data-testid="grade-error" role="alert">
          {error}
        </p>
      )}
      <table>
        <tbody>
          {grades.map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td>{g.grade}</td>
              <td data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.55, Component: Grades });

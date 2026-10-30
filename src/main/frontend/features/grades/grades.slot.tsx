import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import type { Grade } from '../people/peopleQuery';
import { gradesKey, gradesQuery, useGrades } from './gradesQuery';
import type { GradeState } from './gradesQuery';

/** The grades and whether each is retired, then the entry and button a grade is added through: a code of one to three
 *  capital letters not yet a grade (anything else is refused). Then the select and button a grade is retired through. A grade already
 *  retired is refused. Then the two selects and button a steward merges a grade into another through: everyone on the
 *  merged grade today is given the other from the sheet's today. */
function Grades({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const grades = useQuery(gradesQuery(sheetId));
  const codes = useGrades(sheetId);
  const [grade, setGrade] = useState<Grade>('A');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const [code, setCode] = useState('');
  const add = useMutation({
    mutationFn: (body: { grade: string }) =>
      postJson<GradeState[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades`, body, user),
    onSuccess: (list) => {
      queryClient.setQueryData(gradesKey(sheetId), list);
      setCode('');
    },
    onError: (e: Error) => setError(e.message || 'The grade could not be added.'),
  });

  const retire = useMutation({
    mutationFn: (body: { grade: Grade }) =>
      postJson<GradeState[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, body, user),
    onSuccess: (list) => queryClient.setQueryData(gradesKey(sheetId), list),
    onError: (e: Error) => setError(e.message || 'The grade could not be retired.'),
  });

  const [mergeFrom, setMergeFrom] = useState<Grade>('A');
  const [mergeInto, setMergeInto] = useState<Grade>('B');
  const [merged, setMerged] = useState('');
  const merge = useMutation({
    mutationFn: (body: { from: Grade; into: Grade }) =>
      postJson<{ from: Grade; into: Grade; people: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/merge`, body, user),
    onSuccess: async (m) => {
      // People's grades and every figure may have moved.
      await queryClient.invalidateQueries();
      setMerged(`${m.from} into ${m.into}: ${m.people}`);
    },
    onError: (e: Error) => setError(e.message || 'The grade could not be merged.'),
  });

  const submitMerge = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setMerged('');
    once(() => merge.mutateAsync({ from: mergeFrom, into: mergeInto }));
  };

  const submitAdd = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => add.mutateAsync({ grade: code }));
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => retire.mutateAsync({ grade }));
  };

  return (
    <section data-testid="grades">
      <h3>Grades</h3>
      <table>
        <tbody>
          {(grades.data ?? []).map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td data-testid="grade-code">{g.grade}</td>
              <td>
                Retired: <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form onSubmit={submitAdd}>
        <input
          data-testid="grade-add-code"
          placeholder="Code"
          value={code}
          onChange={(e) => {
            setCode(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="grade-add" disabled={add.isPending}>
          Add grade
        </button>
      </form>
      <form onSubmit={submit}>
        <select
          data-testid="grade-retire-select"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value as Grade);
            setError('');
          }}
        >
          {codes.map((g) => (
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
          value={mergeFrom}
          onChange={(e) => {
            setMergeFrom(e.target.value as Grade);
            setError('');
          }}
        >
          {codes.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        into{' '}
        <select
          data-testid="grade-merge-into"
          value={mergeInto}
          onChange={(e) => {
            setMergeInto(e.target.value as Grade);
            setError('');
          }}
        >
          {codes.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="grade-merge" disabled={merge.isPending}>
          Merge grade
        </button>
        {merged && (
          <>
            {' '}
            Merged <span data-testid="grade-merged">{merged}</span>
          </>
        )}
      </form>
      {error && <p data-testid="grade-error">{error}</p>}
      {grades.isError && <p data-testid="grades-load-error">Could not load the grades.</p>}
    </section>
  );
}

// Keyed by sheet, so a chosen grade or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 5.2,
  Component: ({ sheetId }) => <Grades key={`grades-${sheetId}`} sheetId={sheetId} />,
});

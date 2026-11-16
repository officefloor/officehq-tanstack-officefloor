import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey } from '../claims/api';
import { GRADES, peopleKey } from '../people/api';
import { liveKey } from '../sheet/api';
import { addGrade, gradesKey, listGrades, mergeGrade, retireGrade, useGradeCodes } from './api';

/** The sheet's grades, each shown retired or not; an entry and button to add one (one to three capital
 *  letters, not yet a grade), and a select and button to retire one. A retired
 *  grade cannot be chosen for a new person or in an edit; people already on it keep it. A steward can
 *  merge a grade into another (two selects and a button), and sees how many people it regraded. */
export function Grades({ sheetId, user }: { sheetId: string; user: string }) {
  const gradeCodes = useGradeCodes(sheetId);
  const queryClient = useQueryClient();
  const grades = useQuery({ queryKey: gradesKey(sheetId), queryFn: () => listGrades(sheetId) });
  const [grade, setGrade] = useState<string>(GRADES[0]);

  const retire = useMutation({
    mutationFn: (input: string) => retireGrade(sheetId, user, input),
    onSuccess: (data) => {
      queryClient.setQueryData(gradesKey(sheetId), data);
      return queryClient.invalidateQueries({ queryKey: gradesKey(sheetId) });
    },
  });

  const [code, setCode] = useState('');
  const add = useMutation({
    mutationFn: (input: string) => addGrade(sheetId, user, input),
    onSuccess: (data) => {
      setCode('');
      queryClient.setQueryData(gradesKey(sheetId), data);
      queryClient.setQueryData(liveKey(gradesKey(sheetId), true), data);
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: gradesKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: liveKey(gradesKey(sheetId), true) }),
      ]);
    },
  });

  const [mergeFrom, setMergeFrom] = useState<string>(GRADES[0]);
  const [mergeInto, setMergeInto] = useState<string>(GRADES[1]);
  const merge = useMutation({
    mutationFn: (v: { from: string; into: string }) => mergeGrade(sheetId, user, v.from, v.into),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
      queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    ]),
  });

  const submitOnce = useSubmitOnce();
  const addOnce = useSubmitOnce();
  const mergeOnce = useSubmitOnce();

  return (
    <section>
      <h3>Grades</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = code;
          addOnce(() => {
            add.reset();
            retire.reset();
            merge.reset();
            return add.mutateAsync(input);
          });
        }}
      >
        <input data-testid="grade-add-code" value={code} placeholder="New grade (e.g. D)"
          onChange={(e) => {
            setCode(e.target.value);
            add.reset();
          }} />{' '}
        <button type="submit" data-testid="grade-add" disabled={add.isPending}>
          Add grade
        </button>
        {add.isError && (
          <span data-testid="grade-error">A grade is one to three capital letters, not yet a grade.</span>
        )}
      </form>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = grade;
          submitOnce(() => {
            retire.reset();
            merge.reset();
            add.reset();
            return retire.mutateAsync(input);
          });
        }}
      >
        <select data-testid="grade-retire-select" value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            retire.reset();
          }}>
          {gradeCodes.map((g) => <option key={g} value={g}>{g}</option>)}
        </select>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {retire.isError && (
          <span data-testid="grade-error">That grade is already retired.</span>
        )}
      </form>
      <div>
        Merge{' '}
        <select data-testid="grade-merge-from" value={mergeFrom}
          onChange={(e) => {
            setMergeFrom(e.target.value);
            merge.reset();
          }}>
          {gradeCodes.map((g) => <option key={g} value={g}>{g}</option>)}
        </select>{' '}
        into{' '}
        <select data-testid="grade-merge-into" value={mergeInto}
          onChange={(e) => {
            setMergeInto(e.target.value);
            merge.reset();
          }}>
          {gradeCodes.map((g) => <option key={g} value={g}>{g}</option>)}
        </select>{' '}
        <button type="button" data-testid="grade-merge" disabled={merge.isPending}
          onClick={() => {
            const v = { from: mergeFrom, into: mergeInto };
            mergeOnce(() => {
              merge.reset();
              retire.reset();
              add.reset();
              return merge.mutateAsync(v);
            });
          }}>
          Merge grade
        </button>
        {merge.isSuccess && (
          <>
            {' '}Regraded <span data-testid="grade-merged">{merge.data.merged}</span>
          </>
        )}
        {merge.isError && (
          <span data-testid="grade-error">The grade could not be merged: pick two different grades (stewards only).</span>
        )}
      </div>
      <table>
        <tbody>
          {(grades.data ?? []).map((g) => (
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

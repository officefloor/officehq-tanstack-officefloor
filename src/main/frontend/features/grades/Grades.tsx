import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES } from '../people/people';
import { gradesKey, useGrades } from './grades';
import type { Grade } from './grades';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's grades, each showing whether it is retired, and the select and button a grade is retired with; a grade
// already retired is refused. A steward merges a grade into another with two selects and a button: everyone on the
// merged grade today is given the other from today (the same grade twice, or anyone but a steward, is refused); the
// count of people moved shows once it is done.
export function Grades() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const grades = useGrades().data ?? [];
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [mergeFrom, setMergeFrom] = useState<string>(GRADES[0]);
  const [mergeInto, setMergeInto] = useState<string>(GRADES[1]);
  const [merged, setMerged] = useState<number | undefined>(undefined);
  const [error, setError] = useState('');
  // loading a sheet (again, even the same one) clears the last merge's count
  useEffect(() => setMerged(undefined), [sheet]);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const retire = useMutation({
    mutationFn: (input: { grade: string }) =>
      postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades/retire?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setError('');
      queryClient.setQueryData(gradesKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: gradesKey(sheet) });
    },
    onError: () => setError('That grade could not be retired: it is already retired.'),
  });

  const merge = useMutation({
    mutationFn: (input: { from: string; into: string }) =>
      postJson<{ merged: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/grades/merge?user=${encodeURIComponent(user)}`,
        input,
      ),
    onSuccess: async (result) => {
      setError('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
      setMerged(result.merged);
    },
    onError: () => setError('That grade could not be merged.'),
  });

  if (!user || !sheet) return null;

  return (
    <section data-testid="grades">
      <ul data-testid="grade-list">
        {grades.map((g) => (
          <li key={g.grade} data-testid={`grade-row-${g.grade}`}>
            <span data-testid="grade-code">{g.grade}</span> retired:{' '}
            <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
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
      </select>
      <button
        type="button"
        data-testid="grade-retire"
        disabled={retire.isPending}
        onClick={() => {
          setError('');
          once(() => retire.mutateAsync({ grade }));
        }}
      >
        Retire grade
      </button>
      <select
        data-testid="grade-merge-from"
        value={mergeFrom}
        onChange={(e) => {
          setMergeFrom(e.target.value);
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
        value={mergeInto}
        onChange={(e) => {
          setMergeInto(e.target.value);
          setError('');
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="grade-merge"
        disabled={merge.isPending}
        onClick={() => {
          setError('');
          setMerged(undefined);
          once(() => merge.mutateAsync({ from: mergeFrom, into: mergeInto }));
        }}
      >
        Merge grade
      </button>
      {merged !== undefined && (
        <>
          {' '}
          Moved <span data-testid="grade-merged">{merged}</span>
        </>
      )}
      {error && <span data-testid="grade-error">{error}</span>}
    </section>
  );
}

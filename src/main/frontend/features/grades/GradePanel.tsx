import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { claimsKey } from '../claims/useClaims';
import { GRADES, peopleKey } from '../people/usePeople';
import { gradesKey, useGrades, type Grade } from './useGrades';

// The sheet's grades, each shown retired or not, and the select and button that retire one (a grade
// already retired is refused). A retired grade cannot be chosen for anyone; people on it keep it.
// A steward merges a grade into another (the same grade twice, or anyone else, is refused): everyone on
// the merged grade today is given the other from the sheet's today, and how many is shown; pressing
// again removes the old count until the new one is in.
export function GradePanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const grades = useGrades(sheet);
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const retire = useMutation({
    mutationFn: (chosen: string) =>
      postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades/retire`, { grade: chosen, user }),
    onSuccess: (saved) => {
      queryClient.setQueryData(gradesKey(sheet), saved);
    },
  });
  const [from, setFrom] = useState<string>(GRADES[0]);
  const [into, setInto] = useState<string>(GRADES[1]);
  const merge = useMutation({
    mutationFn: (chosen: { from: string; into: string }) =>
      postJson<{ merged: number }>(`/api/sheets/${encodeURIComponent(sheet)}/grades/merge`, { ...chosen, user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });
  return (
    <>
      <ul data-testid="grade-list">
        {grades.map((g) => (
          <li key={g.grade} data-testid={`grade-row-${g.grade}`}>
            {g.grade} retired: <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
      <select
        data-testid="grade-retire-select"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          retire.reset();
          merge.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="grade-retire"
        onClick={() =>
          submitOnce(() => {
            retire.reset();
            merge.reset();
            return retire.mutateAsync(grade);
          })
        }
      >
        Retire grade
      </button>
      <p>
        <select
          data-testid="grade-merge-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            merge.reset();
            retire.reset();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>{' '}
        into{' '}
        <select
          data-testid="grade-merge-into"
          value={into}
          onChange={(e) => {
            setInto(e.target.value);
            merge.reset();
            retire.reset();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>{' '}
        <button
          type="button"
          data-testid="grade-merge"
          onClick={() =>
            submitOnce(() => {
              merge.reset();
              retire.reset();
              return merge.mutateAsync({ from, into });
            })
          }
        >
          Merge grade
        </button>
        {merge.isSuccess && (
          <>
            {' '}
            Moved <span data-testid="grade-merged">{merge.data.merged}</span>
          </>
        )}
      </p>
      {retire.isError && <span data-testid="grade-error" role="alert"> The grade could not be retired: it is already retired.</span>}
      {merge.isError && <span data-testid="grade-error" role="alert"> The grade could not be merged.</span>}
    </>
  );
}

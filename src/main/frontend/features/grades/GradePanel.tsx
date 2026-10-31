import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { GRADES } from '../people/usePeople';
import { gradesKey, useGrades, type Grade } from './useGrades';

// The sheet's grades, each shown retired or not, and the select and button that retire one (a grade
// already retired is refused). A retired grade cannot be chosen for anyone; people on it keep it.
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
            return retire.mutateAsync(grade);
          })
        }
      >
        Retire grade
      </button>
      {retire.isError && <span data-testid="grade-error" role="alert"> The grade could not be retired: it is already retired.</span>}
    </>
  );
}

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES } from '../people/people';
import { gradesKey, useGrades } from './grades';
import type { Grade } from './grades';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's grades, each showing whether it is retired, and the select and button a grade is retired with; a grade
// already retired is refused.
export function Grades() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const grades = useGrades().data ?? [];
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [error, setError] = useState('');
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
      {error && <span data-testid="grade-error">{error}</span>}
    </section>
  );
}

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { GRADES } from '../people/people';
import { type Grade, gradesKey, useGrades } from './grades';

// The sheet's grades, whether each is retired, and the select a grade is retired with. A grade already
// retired is refused.
function Grades() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const grades = useGrades(sheet);
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const queryClient = useQueryClient();

  const retire = useMutation({
    mutationFn: (g: string) =>
      postJson<Grade>(`/api/sheets/${encodeURIComponent(sheet)}/grades/retire`, { grade: g, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: gradesKey(sheet) });
    },
  });
  const submit = useSubmitOnce(retire);

  return (
    <section data-testid="grades">
      <h2>Grades</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>Retired</th>
          </tr>
        </thead>
        <tbody>
          {(grades.data ?? []).map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td data-testid="grade-code">{g.grade}</td>
              <td data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        <select
          data-testid="grade-retire-select"
          aria-label="Grade to retire"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            retire.reset();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        <button
          type="button"
          data-testid="grade-retire"
          disabled={retire.isPending}
          onClick={() => submit(grade)}
        >
          Retire grade
        </button>
        {retire.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}
            {retire.error instanceof HttpError && retire.error.reason
              ? `${retire.error.reason}.`
              : 'The grade was refused.'}
          </span>
        )}
      </p>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 40.2, Component: Grades });

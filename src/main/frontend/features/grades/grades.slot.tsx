import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { gradesKey, useGrades, type Grade } from './useGrades';

/**
 * The sheet's grades, each shown retired or not, and a select to retire one. A retired grade cannot
 * be chosen for a person any more (the people already on it keep it); retiring one twice is refused.
 */
function GradesPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: grades = [] } = useGrades(sheet);
  const [grade, setGrade] = useState('A');

  const submitOnce = useSubmitOnce();
  const retire = useMutation({
    mutationFn: (g: string) => postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheet)}/grades/retire`, { grade: g, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: gradesKey(sheet) });
    },
  });

  return (
    <section data-testid="grades-panel">
      <h2>Grades</h2>
      <form
        data-testid="grade-retire-form"
        onSubmit={(e) => {
          e.preventDefault();
          retire.reset();
          submitOnce(() => retire.mutateAsync(grade));
        }}
      >
        <label>
          Grade{' '}
          <select
            data-testid="grade-retire-select"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              retire.reset();
            }}
          >
            {grades.map((g) => (
              <option key={g.grade} value={g.grade}>
                {g.grade}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {retire.isError && (
          <span data-testid="grade-error" role="alert">
            {' '}
            Could not retire this grade.
          </span>
        )}
      </form>
      <table data-testid="grade-list">
        <tbody>
          {grades.map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td data-testid="grade-code">{g.grade}</td>
              <td data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 7, Component: GradesPanel });

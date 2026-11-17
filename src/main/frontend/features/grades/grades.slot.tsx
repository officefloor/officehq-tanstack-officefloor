import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { GRADES } from '../people/people';
import { gradesKey, useGrades, type Grade } from './grades';

/** The sheet's grades: the select and button that retire one, and each grade with whether it is retired. A grade
 *  already retired is refused. */
function Grades({ sheetId, user }: { sheetId: string; user: string }) {
  const grades = useGrades(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>(GRADES[0]);

  const submit = useSubmitOnce();
  const retire = useMutation({
    mutationFn: (g: string) =>
      postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, { grade: g, by: user }),
    onSuccess: (data) => {
      queryClient.setQueryData(gradesKey(sheetId), data);
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="grades">
      <h3>Grades</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          retire.reset();
          retire.mutate(grade);
        }}
      >
        <select
          data-testid="grade-retire-select"
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
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {retire.isError && <span data-testid="grade-error"> The grade is already retired.</span>}
      </form>
      {grades.isError && <p data-testid="grades-load-error">Could not load grades.</p>}
      <table>
        <tbody>
          {(grades.data ?? []).map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td>{g.grade}</td>
              <td>
                Retired <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 5.5, Component: Grades });

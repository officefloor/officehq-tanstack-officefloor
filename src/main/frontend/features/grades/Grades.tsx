import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { GRADES } from '../people/api';
import { gradesKey, listGrades, retireGrade } from './api';

/** The sheet's grades, each shown retired or not, and a select and button to retire one. A retired
 *  grade cannot be chosen for a new person or in an edit; people already on it keep it. */
export function Grades({ sheetId, user }: { sheetId: string; user: string }) {
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

  const submitOnce = useSubmitOnce();

  return (
    <section>
      <h3>Grades</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = grade;
          submitOnce(() => {
            retire.reset();
            return retire.mutateAsync(input);
          });
        }}
      >
        <select data-testid="grade-retire-select" value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            retire.reset();
          }}>
          {GRADES.map((g) => <option key={g} value={g}>{g}</option>)}
        </select>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {retire.isError && (
          <span data-testid="grade-error">That grade is already retired.</span>
        )}
      </form>
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

import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { GRADES } from '../people/peopleQuery';
import type { Grade } from '../people/peopleQuery';
import { gradesKey, gradesQuery } from './gradesQuery';
import type { GradeState } from './gradesQuery';

/** The grades and whether each is retired, and the select and button a grade is retired through. A grade already
 *  retired is refused. */
function Grades({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const grades = useQuery(gradesQuery(sheetId));
  const [grade, setGrade] = useState<Grade>('A');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const retire = useMutation({
    mutationFn: (body: { grade: Grade }) =>
      postJson<GradeState[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, body, user),
    onSuccess: (list) => queryClient.setQueryData(gradesKey(sheetId), list),
    onError: (e: Error) => setError(e.message || 'The grade could not be retired.'),
  });

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
      <form onSubmit={submit}>
        <select
          data-testid="grade-retire-select"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value as Grade);
            setError('');
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
        {error && <p data-testid="grade-error">{error}</p>}
      </form>
      {grades.isError && <p data-testid="grades-load-error">Could not load the grades.</p>}
    </section>
  );
}

// Keyed by sheet, so a chosen grade or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 5.2,
  Component: ({ sheetId }) => <Grades key={`grades-${sheetId}`} sheetId={sheetId} />,
});

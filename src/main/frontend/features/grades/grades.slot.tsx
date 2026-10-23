import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { GRADES } from '../people/people';
import { gradesKey, useGrades } from './grades';
import type { Grade } from './grades';

/** The grades, each with whether it is retired, and the select and button a grade is retired with. People on a
 *  retired grade keep it, but it cannot be chosen for a new person or in an edit; retiring one already retired is
 *  refused. */
function Grades({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const grades = useGrades(sheetId).data ?? [];
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const retire = useMutation({
    mutationFn: (g: string) =>
      postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, { user, grade: g }),
    onSuccess: (saved) => queryClient.setQueryData(gradesKey(sheetId), saved),
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : `Could not retire the grade ${grade}.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (grades.some((g) => g.grade === grade && g.retired)) {
      setError(`The grade ${grade} is already retired.`);
      return;
    }
    if (!once.start()) return;
    retire.mutate(grade);
  };

  return (
    <section data-testid="grades">
      <h2>Grades</h2>
      <form onSubmit={submit}>
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
        </select>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {error && (
          <p data-testid="grade-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <table>
        <tbody>
          {grades.map((g) => (
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

export const contribution = SheetBody.fill({ order: 7.55, Component: Grades });

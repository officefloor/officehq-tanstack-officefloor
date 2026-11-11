import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { useGrades } from './grades';
import type { Grade } from './grades';

// The sheet's grades: a select and a retire button, each grade listed with whether it is retired. A
// grade already retired is refused. People on a retired grade keep it, but it cannot be chosen for a new
// person or in an edit, and a default grade that is retired falls back to A.
function Grades({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const grades = useGrades(sheetId);
  const [grade, setGrade] = useState('A');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const retire = useMutation({
    mutationFn: (body: { grade: string }) =>
      postJson<{ grades: Grade[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] }),
    onError: () => setError('That grade could not be retired.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if ((grades.data ?? []).some((g) => g.grade === grade && g.retired)) {
      setError(`Grade ${grade} is already retired.`);
      return;
    }
    once((done) => retire.mutate({ grade }, { onSettled: done }));
  };

  return (
    <section data-testid="grades">
      <h2>Grades</h2>
      <form onSubmit={submit}>
        <label>
          Grade{' '}
          <select
            data-testid="grade-retire-select"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              setError('');
            }}
          >
            {(grades.data ?? []).map((g) => (
              <option key={g.grade} value={g.grade}>
                {g.grade}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="grade-retire" disabled={retire.isPending}>
          Retire grade
        </button>
        {error && (
          <p role="alert" data-testid="grade-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="grade-list">
        {(grades.data ?? []).map((g) => (
          <li key={g.grade} data-testid={`grade-row-${g.grade}`}>
            {g.grade} retired: <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -6, Component: Grades });

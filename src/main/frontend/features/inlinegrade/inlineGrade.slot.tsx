import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Grade, Person } from '../people/people';
import { useRowError } from '../people/rowError';
import { useUser } from '../session/session';
import { useGradeCodes } from '../grades/grades';

function InlineGradeHead() {
  return <th>Set grade</th>;
}

// A grade select on each person's row (showing the grade in force until another is chosen) and a
// button that records the chosen grade from the sheet's today, exactly as the person form would.
function PersonInlineGrade({ sheetId, person }: { sheetId: string; person: Person }) {
  const GRADES = useGradeCodes(sheetId);
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [chosen, setChosen] = useState<Grade | null>(null);
  const [error, setError] = useRowError(person.id, 'inline-grade');
  const once = useOnce();
  const save = useMutation({
    mutationFn: (grade: Grade) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/grade`,
        { grade },
        user,
      ),
    onSuccess: () => {
      setChosen(null);
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('That person could not be given that grade.'),
  });

  const grade = chosen ?? person.grade;
  const submit = () => {
    setError('');
    once((done) => save.mutate(grade, { onSettled: done }));
  };

  return (
    <td>
      <select
        data-testid="person-inline-grade"
        aria-label="Grade"
        value={grade}
        onChange={(e) => {
          setChosen(e.target.value as Grade);
          setError('');
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>{' '}
      <button type="button" data-testid="person-inline-grade-save" onClick={submit} disabled={save.isPending}>
        Save grade
      </button>
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </td>
  );
}

export const contributions = [
  PersonHead.fill({ order: 5, Component: InlineGradeHead }),
  PersonRow.fill({ order: 5, Component: PersonInlineGrade }),
];

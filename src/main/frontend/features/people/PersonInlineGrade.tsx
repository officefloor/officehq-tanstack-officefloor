import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES } from './people';
import type { Person } from './people';
import { usePersonError } from './personError';

// The row's own grade select and save: gives the person the chosen grade from the sheet's today through
// the person's own edit, exactly as the person form saves it (keeping their name, terms, contact and
// tags, from the version listed), so it is audited and rolled up the same way. A refusal shows on the row.
export function PersonInlineGrade({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [chosen, setChosen] = useState<string | undefined>(undefined);
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();
  const grade = chosen ?? person.grade;

  const save = useMutation({
    mutationFn: (g: string) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}?user=${encodeURIComponent(user)}`, {
        name: person.name,
        grade: g,
        gradeFrom: '',
        terms: person.terms == null ? '' : String(person.terms),
        termsFrom: '',
        contact: person.contact ?? '',
        tags: person.tags.join(','),
        version: person.version,
      }),
    onSuccess: async () => {
      setChosen(undefined);
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
    onError: () => setError('That person could not be given the grade.'),
  });

  return (
    <td>
      <select
        data-testid="person-inline-grade"
        value={grade}
        onChange={(e) => {
          setChosen(e.target.value);
          setError('');
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
        data-testid="person-inline-grade-save"
        disabled={save.isPending}
        onClick={() => {
          setError('');
          once(() => save.mutateAsync(grade));
        }}
      >
        Save grade
      </button>
    </td>
  );
}

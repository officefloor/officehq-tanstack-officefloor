import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/claims';
import { poolsKey } from '../pools/pools';
import { GRADES, peopleKey } from './people';
import { usePersonError } from './rowError';
import type { Person } from './people';

/** A grade select and save button on the person's row: saves the chosen grade from the sheet's today exactly as
 *  the person form's edit would (name, terms and contact kept), refused when the person has been saved since the
 *  version the row shows. */
function PersonInlineGrade({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState(person.grade);
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  // follow the grade the row shows once it changes (a save here, or another's re-loaded)
  useEffect(() => setGrade(person.grade), [person.grade, person.version]);

  const save = useMutation({
    mutationFn: (body: { user: string; grade: string; version: number }) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/grade`, body),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : `Could not grade ${person.name}.`),
    onSettled: once.done,
  });

  return (
    <td>
      <select
        data-testid="person-inline-grade"
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
      <button
        type="button"
        data-testid="person-inline-grade-save"
        disabled={save.isPending}
        onClick={() => {
          begin();
          if (!once.start()) return;
          save.mutate({ user, grade, version: person.version });
        }}
      >
        Save grade
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 2, Component: PersonInlineGrade });

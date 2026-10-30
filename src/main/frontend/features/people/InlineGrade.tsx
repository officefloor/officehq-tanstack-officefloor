import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { tagList } from '../claims/useClaims';
import { usePersonAction } from './usePersonAction';
import { GRADES, peopleKey, type Person } from './usePeople';

// The row's grade select and its save: records the chosen grade from the sheet's today exactly as
// the person form would (the person's other values as the row loaded them, with the version it
// loaded, so a save over someone else's newer save is refused). Saving the grade the row shows
// records nothing. A refusal shows the person's error.
export function InlineGrade({ person, shown }: { person: Person; shown: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [grade, setGrade] = useState(shown);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  useEffect(() => setGrade(shown), [shown, person.version]);

  const save = useMutation({
    mutationFn: (chosen: string) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(person.id)}`, {
        name: person.name,
        grade: chosen,
        gradeFrom: '',
        terms: person.baseTerms,
        contact: person.contact,
        tags: tagList(person.tags),
        user,
        version: person.version,
      }),
    onSuccess: () => {
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });
  const taken = usePersonAction(person.id, () => save.reset());

  return (
    <>
      <select
        data-testid="person-inline-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          save.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-inline-grade-save"
        onClick={() => {
          taken();
          once(() => save.mutateAsync(grade));
        }}
      >
        Save grade
      </button>
      {save.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The grade could not be saved.
        </span>
      )}
    </>
  );
}

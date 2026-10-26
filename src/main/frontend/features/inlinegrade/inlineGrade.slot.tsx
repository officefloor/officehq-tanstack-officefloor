import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

const GRADES = ['A', 'B', 'C'] as const;

function InlineGradeHeads() {
  return <th>Set grade</th>;
}

// A grade select and the button that saves it: the person's edit, exactly as the person form would save it with
// only the grade changed and a blank from-date (the sheet's today) — so it is refused, nothing saved, when someone
// else saved the person since the people were read. A refusal shows on the person until the next action on their row.
function InlineGradeCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState(person?.grade ?? 'A');
  useEffect(() => {
    if (person) setGrade(person.grade);
  }, [person?.grade]);
  const save = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (chosen: string) => {
      if (!person) throw new Error('No such person');
      return putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}`, {
        name: person.name,
        grade: chosen,
        gradeFrom: '',
        terms: person.ownTerms == null ? '' : String(person.ownTerms),
        termsFrom: '',
        contact: person.contact ?? '',
        tags: person.tags.join(', '),
        version: person.version,
        by: user ?? '',
      });
    },
    // Re-read either way: a grade moves the dues of the person's claims, and a refusal may be a stale version.
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  const last = useLastPersonAction(sheetId, personId, save.submittedAt);

  return (
    <td>
      <select
        data-testid="person-inline-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          save.reset();
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
        onClick={() => save.mutate(grade)}
      >
        Save grade
      </button>
      {save.isError && last && (
        <span data-testid="person-error" role="alert">
          {save.error.message}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 35, Component: InlineGradeHeads }),
  PersonCells.fill({ order: 35, Component: InlineGradeCells }),
];

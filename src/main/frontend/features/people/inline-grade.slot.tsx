import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey } from '../sheet/sheet';
import { GRADES, type Person, personActionKey, useLatestPersonAction } from './people';

function InlineGradeHead() {
  return <th>Regrade</th>;
}

// Records the chosen grade from the sheet's today exactly as the person form would: it saves the
// person's own values with only the grade changed, over the version the row shows, so a save over
// someone else's edit is refused (and so is one by a viewer). A refusal shows its error until the
// grade is changed or the next action on the row is taken.
function PersonInlineGrade({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  // null follows the grade the row shows until another is chosen
  const [chosen, setChosen] = useState<string | null>(null);
  const grade = chosen ?? person.grade;
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (g: string) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}`, {
        name: person.name,
        grade: g,
        gradeFrom: '',
        terms: person.terms === null ? '' : String(person.terms),
        contact: person.contact ?? '',
        tags: person.tags.join(', '),
        user,
        version: person.version,
      }),
    onSuccess: async () => {
      // A grade moves the person's figures and their claims' dues, so the whole sheet is re-read.
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
      setChosen(null);
    },
  });
  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <select
        data-testid="person-inline-grade"
        aria-label={`Grade for ${person.name}`}
        value={grade}
        onChange={(e) => {
          save.reset();
          setChosen(e.target.value);
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
          save.reset();
          save.mutate(grade);
        }}
      >
        Save grade
      </button>
      {save.isError && save.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The grade was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 0.6, Component: InlineGradeHead }),
  PersonColumns.fill({ order: 0.6, Component: PersonInlineGrade }),
];

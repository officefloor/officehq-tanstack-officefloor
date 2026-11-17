import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { GRADES } from '../people/people';
import { gradesKey, useGrades, type Grade } from './grades';

/** The sheet's grades: the select and button that retire one, the selects and button a steward merges one into another
 *  with, and each grade with whether it is retired. A grade already retired is refused; so is a merge of a grade into
 *  itself, or by anyone but a steward. A merge gives everyone on the merged grade today the other, each as the person
 *  form would, and shows how many it changed; the count is cleared while a new press runs. */
function Grades({ sheetId, user }: { sheetId: string; user: string }) {
  // keyed by sheet, so a count or error shown for one sheet is gone once another is loaded
  return <SheetGrades key={sheetId} sheetId={sheetId} user={user} />;
}

function SheetGrades({ sheetId, user }: { sheetId: string; user: string }) {
  const grades = useGrades(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const [mergeFrom, setMergeFrom] = useState<string>(GRADES[0]);
  const [mergeInto, setMergeInto] = useState<string>(GRADES[1]);

  const submit = useSubmitOnce();
  const retire = useMutation({
    mutationFn: (g: string) =>
      postJson<Grade[]>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/retire`, { grade: g, by: user }),
    onSuccess: (data) => {
      queryClient.setQueryData(gradesKey(sheetId), data);
    },
    onSettled: submit.end,
  });
  const merge = useMutation({
    mutationFn: () =>
      postJson<{ merged: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/grades/merge`, {
        from: mergeFrom,
        into: mergeInto,
        by: user,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: submit.end,
  });
  const resetAll = () => {
    retire.reset();
    merge.reset();
  };

  return (
    <section data-testid="grades">
      <h3>Grades</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          resetAll();
          retire.mutate(grade);
        }}
      >
        <select
          data-testid="grade-retire-select"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            resetAll();
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
      </form>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          resetAll();
          merge.mutate();
        }}
      >
        Merge{' '}
        <select
          data-testid="grade-merge-from"
          value={mergeFrom}
          onChange={(e) => {
            setMergeFrom(e.target.value);
            resetAll();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        into{' '}
        <select
          data-testid="grade-merge-into"
          value={mergeInto}
          onChange={(e) => {
            setMergeInto(e.target.value);
            resetAll();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="grade-merge" disabled={merge.isPending}>
          Merge grade
        </button>
        {merge.isSuccess && (
          <>
            {' '}
            Changed <span data-testid="grade-merged">{merge.data.merged}</span>
          </>
        )}
      </form>
      {retire.isError && <p data-testid="grade-error">The grade is already retired.</p>}
      {merge.isError && <p data-testid="grade-error">The grade could not be merged.</p>}
      {grades.isError && <p data-testid="grades-load-error">Could not load grades.</p>}
      <table>
        <tbody>
          {(grades.data ?? []).map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td>{g.grade}</td>
              <td>
                Retired <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 5.5, Component: Grades });

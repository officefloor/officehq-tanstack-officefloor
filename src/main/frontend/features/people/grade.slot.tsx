import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey } from '../sheet/sheet';
import { GRADES, useSelectedPeople } from './people';

type BulkGradeResult = { graded: number };

// Gives the selected people a grade together, from the from-date (the sheet's today when blank): the
// server edits each in list order as the person form would, and the count shows once it answers (gone
// while it runs). A refusal changes nothing.
function BulkGrade() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedPeople();
  const [grade, setGrade] = useState<string>('A');
  const [from, setFrom] = useState('');
  const queryClient = useQueryClient();

  const bulk = useMutation({
    mutationFn: (p: { personIds: number[]; grade: string; gradeFrom: string }) =>
      postJson<BulkGradeResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-grade`, {
        personIds: p.personIds.map(String),
        grade: p.grade,
        gradeFrom: p.gradeFrom,
        user,
      }),
    onSuccess: async () => {
      setSelected([]);
      // Grades move people's figures and their claims' dues, so the whole sheet is re-read.
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
  });
  const submit = useSubmitOnce(bulk);

  return (
    <p>
      <select
        data-testid="bulk-grade"
        aria-label="Grade for the selected people"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          bulk.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>{' '}
      <input
        data-testid="bulk-grade-from"
        type="date"
        aria-label="Grade from (blank for the sheet's today)"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          bulk.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-grade-apply"
        disabled={bulk.isPending}
        onClick={() => submit({ personIds: selected, grade, gradeFrom: from })}
      >
        Grade selected
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Changed <strong data-testid="bulk-graded">{bulk.data.graded}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk grade was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 21, Component: BulkGrade });

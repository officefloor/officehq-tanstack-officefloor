import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { openingsKey } from './useOpenings';

// The opening import box: one opening per line as name,period,amount (a person on the sheet by name or
// alias, a period on the sheet, an amount of 0.00 or more with at most two decimals). Every line is
// checked first; one refused line takes none. Otherwise each line adds its amount to the person's
// carried figure for the period. Then shows how many lines went through; pressing again removes the old
// count until the new one is in.
export function ImportOpenings() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const importOpenings = useMutation({
    mutationFn: (pasted: string) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheet)}/import-openings`, { text: pasted, user }),
    onSuccess: () => {
      setText('');
      // Not awaited: the count shows as soon as the import is in.
      void queryClient.invalidateQueries({ queryKey: openingsKey(sheet) });
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          importOpenings.reset();
          return importOpenings.mutateAsync(text);
        });
      }}
    >
      <textarea
        data-testid="opening-import"
        value={text}
        rows={3}
        placeholder="name,period,amount — one opening per line"
        onChange={(e) => {
          setText(e.target.value);
          importOpenings.reset();
        }}
      />{' '}
      <button type="submit" data-testid="opening-import-submit">Import openings</button>
      {importOpenings.isSuccess && (
        <>
          {' '}
          Took <span data-testid="opening-import-done">{importOpenings.data.set}</span>
        </>
      )}
      {importOpenings.isError && (
        <span data-testid="opening-error" role="alert">
          {' '}
          Nothing was taken: every line needs a person on the sheet (by name or alias), a period on the sheet and
          an amount of 0.00 or more with at most two decimals.
        </span>
      )}
    </form>
  );
}

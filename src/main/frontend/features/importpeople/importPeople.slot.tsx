import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { peopleKey } from '../people/usePeople';

/** An import box: lines of name,grade,terms,contact (the last two may be absent or blank) each add a
 *  person, in order, as the person form would; a line naming someone on the sheet (by name or alias)
 *  updates them instead. The server checks every line first: one refused line (or a name or contact
 *  given twice) refuses the import and changes no one. Shows how many were added and how many updated
 *  (gone while it runs). */
function ImportPeople({ sheetId }: { sheetId: string }) {
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const importing = useMutation({
    mutationFn: (t: string) => postJson<{ added: number; updated: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/import`, { text: t }),
    // The count shows as soon as the import is done; the new rows follow once re-read.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  return (
    <form
      data-testid="import"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          importing.reset();
          return importing.mutateAsync(text);
        });
      }}
    >
      <label>
        Import people (name,grade,terms,contact per line){' '}
        <textarea
          data-testid="import-people"
          rows={4}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            importing.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="import-submit" disabled={importing.isPending}>
        Import people
      </button>
      {importing.isSuccess && (
        <>
          {' '}
          Added <strong data-testid="import-added">{importing.data.added}</strong>, updated{' '}
          <strong data-testid="import-updated">{importing.data.updated}</strong>
        </>
      )}
      {importing.isError && <p data-testid="import-error">{importing.error.message}</p>}
    </form>
  );
}

export const contribution = SheetBody.fill({
  order: 11,
  Component: ({ sheetId }) => <ImportPeople sheetId={sheetId} />,
});

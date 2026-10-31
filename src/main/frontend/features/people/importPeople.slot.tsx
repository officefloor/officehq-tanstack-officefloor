import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { peopleKey } from './usePeople';

/**
 * Paste lines of name,grade,terms,contact (the last two may be absent or blank): a person per line,
 * in order. A line naming a person on the sheet (by name or alias) updates them as the person form
 * would — the grade from the sheet's today, the terms and contact when given; any other line adds a
 * person. Every line is checked by the form's rules first, and a name or contact repeated in the
 * paste is refused: one refused line refuses the whole import. Shows how many were added and how
 * many updated; the counts go while the next press runs.
 */
function ImportPeople({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const importing = useMutation({
    mutationFn: (t: string) =>
      postJson<{ added: number; updated: number }>(`/api/sheets/${encodeURIComponent(sheet)}/import-people`, { text: t, user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  return (
    <section data-testid="import-panel">
      <label>
        Import people (name,grade,terms,contact per line)
        <br />
        <textarea
          data-testid="import-people"
          rows={4}
          cols={50}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            importing.reset();
          }}
        />
      </label>
      <br />
      <button
        type="button"
        data-testid="import-submit"
        disabled={importing.isPending}
        onClick={() => {
          importing.reset();
          importing.mutate(text);
        }}
      >
        Import people
      </button>
      {importing.isSuccess && (
        <span>
          {' '}
          Added <strong data-testid="import-added">{importing.data.added}</strong>, updated{' '}
          <strong data-testid="import-updated">{importing.data.updated}</strong>
        </span>
      )}
      {importing.isError && (
        <span data-testid="import-error" role="alert">
          {' '}
          Nothing was imported:{' '}
          {importing.error instanceof HttpError && importing.error.body
            ? importing.error.body
            : 'every line needs a person the person form would accept.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 10.5, Component: ImportPeople });

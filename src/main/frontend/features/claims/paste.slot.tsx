import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from './useClaims';

/**
 * Paste lines of name,amount,date,note: a claim per line, in order, for the person of that name
 * (the note may hold commas, the date may be blank for the sheet's today, the note absent), opened
 * as the open button would. A line whose date and note match a DRAFT of that person updates the
 * newest such DRAFT to its amount and opens it instead; a match with no DRAFT refuses the paste.
 * A note ending in ` #<reference>` gives the claim that external reference instead (not part of the note).
 * One refused line (by the claim form's rules or the open button's), or a reference already
 * used on the sheet or repeated in the paste, refuses the whole paste.
 * Shows how many lines went through; the count goes while the next press runs.
 */
function PasteClaims({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const paste = useMutation({
    mutationFn: (t: string) =>
      postJson<{ added: number }>(`/api/sheets/${encodeURIComponent(sheet)}/paste-claims`, { text: t, user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });
  return (
    <section data-testid="paste-panel">
      <label>
        Paste claims (name,amount,date,note per line)
        <br />
        <textarea
          data-testid="paste-claims"
          rows={4}
          cols={50}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            paste.reset();
          }}
        />
      </label>
      <br />
      <button
        type="button"
        data-testid="paste-submit"
        disabled={paste.isPending}
        onClick={() => {
          paste.reset();
          paste.mutate(text);
        }}
      >
        Add and open pasted claims
      </button>
      {paste.isSuccess && (
        <span>
          {' '}
          Went through <strong data-testid="paste-added">{paste.data.added}</strong>
        </span>
      )}
      {paste.isError && (
        <span data-testid="paste-error" role="alert">
          {' '}
          Nothing was added:{' '}
          {paste.error instanceof HttpError && paste.error.body
            ? paste.error.body
            : 'every line needs a name on the sheet and a claim the claim form would accept.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 14, Component: PasteClaims });

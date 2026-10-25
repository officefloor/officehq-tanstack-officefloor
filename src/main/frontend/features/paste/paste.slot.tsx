import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';
import { claimsKey } from '../claims/useClaims';

type PasteResult = { added: number };

// The paste box: lines of name,amount,date,note (the note may hold commas; a blank date is the sheet's today;
// the note may be absent) add a DRAFT claim per line, in order, for the named person. The server checks every
// line with the claim form's rules first, so a refusal adds nothing. Shows how many were added; the count is
// gone while it runs, and shows before the new rows do.
function PasteClaims({ sheetId }: { sheetId: string; user: string }) {
  const [lines, setLines] = useState('');
  const [added, setAdded] = useState<number | null>(null);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const paste = useMutation({
    mutationFn: async (text: string) => {
      setAdded(null);
      if (text.trim() === '') throw new Error('Paste at least one line of name,amount,date,note');
      // `by` is the signed-in user the change is recorded against.
      const result = await postJson<PasteResult>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/paste`, {
        lines: text,
        by: user ?? '',
      });
      setAdded(result.added);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      return result;
    },
  });

  const submit = useSubmitOnce(paste);
  return (
    <form
      data-testid="paste-box"
      onSubmit={(e) => {
        e.preventDefault();
        submit(lines);
      }}
    >
      <label>
        Paste claims (name,amount,date,note per line)
        <br />
        <textarea
          data-testid="paste-claims"
          rows={4}
          cols={50}
          value={lines}
          onChange={(e) => {
            setLines(e.target.value);
            paste.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Add draft claims
      </button>{' '}
      {added !== null && !paste.isError && (
        <span>
          Added <strong data-testid="paste-added">{added}</strong>
        </span>
      )}
      {paste.isError && (
        <p data-testid="paste-error" role="alert">
          {paste.error.message}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 17, Component: PasteClaims });

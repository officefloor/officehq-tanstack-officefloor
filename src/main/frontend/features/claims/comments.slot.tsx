import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The claim's comments, oldest first, each as "<user> on <date>: <text>", and the entry and button that add one. The
 * server refuses an empty comment, and the refusal shows on the claim until its next action.
 */
function ClaimComments({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const [text, setText] = useState('');
  const once = useSubmitOnce();
  const comment = useMutation({
    mutationFn: (body: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/comments`, { text: body }, user),
    onSuccess: () => {
      setText('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The comment could not be added.'),
  });

  return (
    <td data-testid="claim-comments">
      {(claim.comments ?? []).map((line, i) => (
        <div key={i} data-testid="claim-comments-line">
          {line}
        </div>
      ))}
      <input
        data-testid="claim-comment-text"
        placeholder="comment"
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-comment-add"
        disabled={comment.isPending}
        onClick={() => {
          setError('');
          once(() => comment.mutateAsync(text));
        }}
      >
        Comment
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 91, Component: ClaimComments });

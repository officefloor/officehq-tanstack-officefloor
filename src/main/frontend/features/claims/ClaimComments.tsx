import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from './claimError';

// The claim's comments, oldest first, and the box to add one: kept with its writer and the sheet's today. The server
// refuses an empty comment (and any change by a viewer or on an archived sheet); the refusal shows as the claim's error.
export function ClaimComments({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/comments?user=${encodeURIComponent(user)}`, body),
    onSuccess: async () => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () => setError('This comment cannot be added (type a comment; a viewer cannot change the sheet).'),
  });

  return (
    <>
      <ol>
        {(claim.comments ?? []).map((c, i) => (
          <li key={i} data-testid="claim-comments-line">
            {`${c.by} on ${c.date}: ${c.text}`}
          </li>
        ))}
      </ol>
      <input
        data-testid="claim-comment-text"
        placeholder="Comment"
        value={text}
        onChange={(e) => {
          setError('');
          setText(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-comment-add"
        disabled={add.isPending}
        onClick={() => {
          setError('');
          if (text.trim() === '') {
            setError('Type a comment.');
            return;
          }
          once(() => add.mutateAsync({ text }));
        }}
      >
        Comment
      </button>
    </>
  );
}

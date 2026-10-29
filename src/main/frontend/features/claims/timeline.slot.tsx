import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimColumns';
import type { Claim } from './claimsQuery';

/**
 * The claim's timeline: the button lists the audit lines about it, newest first, exactly as the audit file holds them
 * (by its number, or for a claim with no number by its person, amount and date together). Each press re-reads them,
 * and the old list is gone while it does.
 */
function ClaimTimeline({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const [lines, setLines] = useState<string[]>([]);
  const timeline = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/timeline`),
    onSuccess: setLines,
  });

  return (
    <td data-testid="claim-timeline-cell">
      <button
        type="button"
        data-testid="claim-timeline"
        disabled={timeline.isPending}
        onClick={() => {
          setLines([]);
          timeline.mutate();
        }}
      >
        Timeline
      </button>
      {lines.map((line, i) => (
        <div key={i} data-testid="claim-timeline-line">
          {line}
        </div>
      ))}
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 96, Component: ClaimTimeline });

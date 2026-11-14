import { useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import type { Claim } from './claims';

// The claim's timeline: the audit lines about it, newest first, as the file holds them — those carrying its number,
// or for a claim with no number those naming its person, amount and date together.
// Read on each press of the button; the old lines are gone while it reads.
export function ClaimTimeline({ claim }: { claim: Claim }) {
  const [sheet] = useCurrentSheet();
  const [lines, setLines] = useState<string[]>([]);
  const pressed = useRef(0);

  const show = useMutation({
    mutationFn: async (press: number) => ({
      press,
      read: await getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/timeline`),
    }),
    // Only the latest press's answer is shown; an earlier one that arrives late is dropped.
    onSuccess: ({ press, read }) => {
      if (press === pressed.current) setLines(read);
    },
  });

  return (
    <>
      <button
        type="button"
        data-testid="claim-timeline"
        onClick={() => {
          setLines([]);
          pressed.current += 1;
          show.mutate(pressed.current);
        }}
      >
        Timeline
      </button>
      <ol>
        {lines.map((line, i) => (
          <li key={i} data-testid="claim-timeline-line">
            {line}
          </li>
        ))}
      </ol>
    </>
  );
}

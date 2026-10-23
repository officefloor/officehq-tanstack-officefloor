import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim's timeline: the audit lines about it exactly as the audit file holds them, newest first (those carrying its
 *  number, or for a claim with none those naming its person, amount and date). Each press of the button re-reads the
 *  file. */
export function ClaimTimeline({ sheetId, claimId }: { sheetId: string; claimId: string | number }) {
  const [shown, setShown] = useState(0);
  const lines = useQuery({
    queryKey: ['sheet', sheetId, 'claim-timeline', String(claimId), shown],
    queryFn: () =>
      getJson<string[]>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(String(claimId))}/timeline`,
      ),
    enabled: shown > 0,
  });

  return (
    <>
      <button type="button" data-testid="claim-timeline" onClick={() => setShown((n) => n + 1)}>
        Timeline
      </button>
      {shown > 0 && lines.data && (
        <ol>
          {lines.data.map((line, i) => (
            <li key={i} data-testid="claim-timeline-line">
              {line}
            </li>
          ))}
        </ol>
      )}
    </>
  );
}

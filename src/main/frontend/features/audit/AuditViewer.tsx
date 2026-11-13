import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// The audit viewer: the sheet's audit lines as the file holds them, newest first. A name in the filter
// keeps only the lines about that person (name= or person=, case and surrounding spaces ignored), followed, for a
// person transferred here, by the lines about them from the sheet they came from (newest first).
// Read on each press of the show button; the old lines are gone while it reads.
export function AuditViewer() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [filter, setFilter] = useState('');
  const [lines, setLines] = useState<string[]>([]);
  const pressed = useRef(0);

  const show = useMutation({
    mutationFn: async (input: { name: string; press: number }) => ({
      press: input.press,
      read: await getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/audit?name=${encodeURIComponent(input.name)}`),
    }),
    // Only the latest press's answer is shown; an earlier one that arrives late is dropped.
    onSuccess: ({ press, read }) => {
      if (press === pressed.current) setLines(read);
    },
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setLines([]);
    pressed.current += 1;
    show.mutate({ name: filter.trim(), press: pressed.current });
  };

  return (
    <section data-testid="audit-viewer">
      <form onSubmit={submit}>
        <input
          data-testid="audit-filter"
          placeholder="Person (blank for all)"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <button type="submit" data-testid="audit-show">
          Show audit
        </button>
      </form>
      <ul data-testid="audit-lines">
        {lines.map((line, i) => (
          <li key={i} data-testid="audit-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

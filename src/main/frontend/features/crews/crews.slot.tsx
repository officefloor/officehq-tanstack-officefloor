import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { peopleKey, usePeople } from '../people/people.slot';
import { useDueTotals } from '../rates/rates.slot';
import { showHundredths } from '../rates/useRates';
import { useUser } from '../session/useSession';

/** A person of a crew: their id and name. */
export type CrewPerson = { personId: number; name: string };

/** A crew of the sheet: its name, its lead and its members in the order added. */
export type Crew = { name: string; lead: CrewPerson; members: CrewPerson[] };

/** The key the sheet's crews are read under: below the people's key, so whatever changes the people (a merge, a
 *  rename) re-reads the crews too. */
export const crewsKey = (sheetId: string) => [...peopleKey(sheetId), 'crews'] as const;

/** The sheet's crews, in the order added. */
export function useCrews(sheetId: string) {
  return useQuery({
    queryKey: crewsKey(sheetId),
    queryFn: () => getJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`),
  });
}

// A crew's row: its lead, its due-total (the lead's and the members' due-totals added up, n/a ones skipped), a line per
// member, and a picker of a person that adds one more member. The lead, or a person already in the crew, is refused with
// an error on the crew until the picker is changed or it is submitted again.
function CrewRow({ sheetId, crew }: { sheetId: string; crew: Crew }) {
  const people = usePeople(sheetId).data ?? [];
  const dueTotals = useDueTotals(sheetId);
  const [member, setMember] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (value: string) => {
      if (value === '') throw new Error('Choose a person');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheetId)}/crews/members`, {
        crew: crew.name,
        person: Number(value),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) });
      setMember('');
    },
  });
  const submit = useSubmitOnce(add);

  let total = 0n;
  for (const p of [crew.lead, ...crew.members]) total += dueTotals.get(p.personId) ?? 0n;

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <span data-testid="crew-name">{crew.name}</span>{' '}
      lead <span data-testid="crew-lead">{crew.lead.name}</span>{' '}
      due total <span data-testid="crew-due-total">{showHundredths(total)}</span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.personId} data-testid="crew-member-line">
            <span data-testid="crew-member-name">{m.name}</span>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          add.reset();
          submit(member);
        }}
      >
        <select
          data-testid="crew-member-select"
          value={member}
          onChange={(e) => {
            setMember(e.target.value);
            add.reset();
          }}
        >
          <option value="">Person…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="crew-error" role="alert">
            {' '}
            {add.error.message}
          </span>
        )}
      </form>
    </li>
  );
}

// The crews: a form adds one by name (trimmed) with a lead chosen from the people; a blank name, one already on the
// sheet, or no lead is refused.
function CrewsPanel({ sheetId }: { sheetId: string; user: string }) {
  const crews = useCrews(sheetId);
  const people = usePeople(sheetId).data ?? [];
  const [name, setName] = useState('');
  const [lead, setLead] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (body: { name: string; lead: string }) => {
      if (body.name.trim() === '') throw new Error('Enter a crew name');
      if (body.lead === '') throw new Error('Choose a lead');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`, {
        name: body.name,
        lead: Number(body.lead),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) });
      setName('');
      setLead('');
    },
  });

  const submit = useSubmitOnce(add);
  return (
    <section data-testid="crews-panel">
      <h2>Crews</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          add.reset();
          submit({ name, lead });
        }}
      >
        <input
          data-testid="crew-form-name"
          placeholder="Crew name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />{' '}
        <select
          data-testid="crew-form-lead"
          aria-label="Lead"
          value={lead}
          onChange={(e) => {
            setLead(e.target.value);
            add.reset();
          }}
        >
          <option value="">Lead…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {add.isError && (
          <span data-testid="crew-form-error" role="alert">
            {' '}
            {add.error.message}
          </span>
        )}
      </form>
      <ul data-testid="crew-list">
        {(crews.data ?? []).map((c) => (
          <CrewRow key={c.name} sheetId={sheetId} crew={c} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 8.75, Component: CrewsPanel });

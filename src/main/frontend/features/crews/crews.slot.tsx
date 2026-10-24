import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { useSettings } from '../roles/roles';
import { sumDue, useRates } from '../rates/rates';
import { personDue } from '../rates/due.slot';
import { crewsPath, crewsQueryKey, useCrews, type Crew } from './crews';
import type { Person } from '../people/people';
import type { Claim } from '../claims/claims';
import type { Rate } from '../rates/rates';
import type { Settings } from '../roles/roles';

/** A crew's due-total: its lead's and its members' due-totals added up, n/a skipped. */
export function crewDue(crew: Crew, people: Person[], claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): string {
  const ids = [crew.lead?.id, ...crew.members.map((m) => m.id)];
  return sumDue(people.filter((p) => ids.includes(p.id)).map((p) => personDue(p, claims, rates, today, settings)));
}

// The sheet's crews, each with its lead, its members and its due-total, and the entry that adds a crew with a lead.
// A blank name, or one already a crew's, is refused by the server and shows the error beside the entry, which goes
// as soon as the entry changes.
function Crews() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const crews = useCrews();
  const people = usePeople();
  const [name, setName] = useState('');
  const [leadId, setLeadId] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { name: string; leadId: number | null }) => postJson<Crew[]>(crewsPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsQueryKey(sheet ?? '') });
      setName('');
    },
  });
  const submit = useSubmitOnce(add);

  if (!user || !sheet) return null;
  return (
    <section data-testid="crews">
      <h2>Crews</h2>
      <table>
        <thead>
          <tr><th>Crew</th><th>Lead</th><th>Members</th><th>Due</th><th>Add member</th></tr>
        </thead>
        <tbody>
          {(crews.data ?? []).map((c) => <CrewRow key={c.id} crew={c} />)}
        </tbody>
      </table>
      <form
        data-testid="crew-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ name, leadId: leadId === '' ? null : Number(leadId) });
        }}
      >
        <input
          data-testid="crew-form-name"
          placeholder="Crew name"
          value={name}
          onChange={(e) => { setName(e.target.value); add.reset(); }}
        />
        <select
          data-testid="crew-form-lead"
          value={leadId}
          onChange={(e) => { setLeadId(e.target.value); add.reset(); }}
        >
          <option value="">Choose a lead</option>
          {(people.data ?? []).map((p) => (
            <option key={p.id} value={String(p.id)}>{p.name}</option>
          ))}
        </select>
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {add.isError && (
          <span data-testid="crew-form-error">A crew's name must not be blank or already a crew's, and it needs a lead.</span>
        )}
      </form>
    </section>
  );
}

// A crew's row: its lead, its members, its due-total (the lead's and the members' due-totals added up, n/a skipped)
// and the member select that adds a member. The lead, or a person already in the crew, is refused and shows the error
// on the crew, which goes as soon as the entry changes.
function CrewRow({ crew }: { crew: Crew }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  const [personId, setPersonId] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { personId: number | null }) =>
      postJson<Crew[]>(`${crewsPath(sheet ?? '')}/${crew.id}/members`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(add);
  return (
    <tr data-testid={`crew-row-${crew.name}`}>
      <td data-testid="crew-name">{crew.name}</td>
      <td data-testid="crew-lead">{crew.lead?.name ?? ''}</td>
      <td>
        <ul>
          {crew.members.map((m) => (
            <li key={m.id} data-testid="crew-member-line">{m.name}</li>
          ))}
        </ul>
      </td>
      <td data-testid="crew-due-total">{crewDue(crew, people.data ?? [], claims.data ?? [], rates.data, today, settings.data)}</td>
      <td>
        <select
          data-testid="crew-member-select"
          value={personId}
          onChange={(e) => { setPersonId(e.target.value); add.reset(); }}
        >
          <option value="">Choose a person</option>
          {(people.data ?? []).map((p) => (
            <option key={p.id} value={String(p.id)}>{p.name}</option>
          ))}
        </select>
        <button
          type="button"
          data-testid="crew-member-add"
          disabled={add.isPending}
          onClick={() => submit({ personId: personId === '' ? null : Number(personId) })}
        >
          Add member
        </button>
        {add.isError && (
          <span data-testid="crew-error">A member must be a person not already the crew's lead or in the crew.</span>
        )}
      </td>
    </tr>
  );
}

export const contribution = HomeMain.fill({ order: 6.8, Component: Crews });

import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, postJson, putJson } from '../../api/http';
import { asFlag, asNumber, useSearchParam } from '../../url/useSearchParam';
import { useUser } from '../session/useSession';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { claimsKey, type Claim } from '../claims/useClaims';
import { useSheetToday } from '../today/today.slot';

/** `version` counts the person's saves; an edit sends the one its form loaded. `statedTotal` ("n/a" when it had
 *  none) and `statementDate` are the last statement's, none before the first. `retiredFrom` is the date the person
 *  is retired from; set at all, they count as retired. `held`: whether they have any hold; `holds`: their holds, in the order
 *  added — their OPEN and PENDING claims dated within one count in no live total unless a statement binds them. `patrons`: the names of the people they have named as patrons, sorted; `patronShares`: each patron with their share of
 *  100.00 (two decimals; split equally in the order named, the remainder to the first named), sorted by name. `grade` is the
 *  grade in force on the sheet's today; `grades` every grade they were given, each from its from-date (null: from
 *  the beginning of time). `terms`: the person's own terms in days, null when they have none; a claim added for them
 *  is due by its date plus these, else their grade's terms, else the sheet's. `contact`: the person's contact, trimmed
 *  and lower-cased, null when they have none; no two people on a sheet share one. `aliases`: the other names they go by,
 *  sorted as names are compared (ignoring case). `clashing`: whether their name or an alias is used, as a name or
 *  alias, on another sheet of the sheet's tree (its ancestors and descendants); every edit of them is then refused
 *  until it renames them out of the clash. */
export type Person = { id: number; name: string; grade: string; grades: GradeFrom[]; version: number;
  statementCount: number; statedTotal: string | null; statementDate: string | null; retiredFrom: string | null; held: boolean;
  holds: HoldSpan[];
  patrons: string[]; patronShares: PatronShare[]; terms: number | null; contact: string | null; aliases: string[];
  clashing: boolean };

export type PatronShare = { name: string; share: string };

export type GradeFrom = { grade: string; from: string | null };

/** A hold from its from-date to its to-date (yyyy-mm-dd, both inclusive; null: open at that end). */
export type HoldSpan = { from: string | null; to: string | null };

/** Each person's holds, by person id. */
export type Holds = ReadonlyMap<number, HoldSpan[]>;

/** The person's grade in force on a date (yyyy-mm-dd): the one with the latest from-date on or before it, none
 *  counting as the earliest. Undefined without a person. */
export function gradeOn(person: Person | undefined, date: string): string | undefined {
  if (person === undefined) return undefined;
  let found: GradeFrom | undefined;
  for (const g of person.grades) {
    if (g.from !== null && g.from > date) continue;
    if (found === undefined || (g.from ?? '') > (found.from ?? '')) found = g;
  }
  return found?.grade ?? person.grade;
}

const GRADES = ['A', 'B', 'C'] as const;

const CONTACT_TAKEN = "That contact is already another person's on this sheet";

/** The key every feature reads the sheet's people under. */
export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
  });
}

/** Each person's holds: their OPEN claims dated within one, unless a statement binds them, count in no live total.
 *  From the day a person's retirement date is reached (the sheet's today on or after it), it holds all their dates. */
export function useHeld(sheetId: string): Holds {
  const people = usePeople(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  return useMemo(() => new Map((people.data ?? []).map((p) => [p.id,
    p.retiredFrom !== null && today !== undefined && p.retiredFrom <= today
      ? [...(p.holds ?? []), { from: null, to: null }] : p.holds ?? []])), [people.data, today]);
}

/** Which person's editor is open (search param `editPerson`); none means the form adds. */
export function useEditingPerson() {
  return useSearchParam('editPerson', asNumber);
}

// The person form: adds a person, or — with a person's editor open — saves their changes. It is
// re-mounted for each person opened (see PeoplePanel), so it starts filled with their values and
// remembers the version it loaded. When someone else has saved the person since, the save is
// refused: the form then shows the person's current values and version, so saving again works. Any other refused
// edit closes the editor, nothing saved, and the add form shows the refusal (`refused`) until it is changed or submitted.
function PersonForm({ sheetId, editing, refused, onRefused }: { sheetId: string; editing?: Person;
  refused?: string; onRefused: (message: string | undefined) => void }) {
  const [, setEditing] = useEditingPerson();
  const [name, setName] = useState(editing?.name ?? '');
  const [grade, setGrade] = useState(editing?.grade ?? 'A');
  // The date the grade is given from; blank is the sheet's today (an added person's grade applies
  // from the beginning of time).
  const [gradeFrom, setGradeFrom] = useState('');
  // The person's own terms in days; blank for none.
  const [terms, setTerms] = useState(editing?.terms == null ? '' : String(editing.terms));
  // The person's contact; blank for none.
  const [contact, setContact] = useState(editing?.contact ?? '');
  // A contact another person on the sheet already has, refused before anything is sent (the server
  // refuses it too): the message, until the form is changed or submitted again.
  const [taken, setTaken] = useState<string>();
  const people = usePeople(sheetId);
  const [version, setVersion] = useState(editing?.version);
  // Whether this form is still the one on screen: a save that finishes after another person's editor
  // was opened must not close that editor.
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
  const save = useMutation({
    mutationFn: async (body: { name: string; grade: string; gradeFrom: string; terms: string; contact: string }) => {
      if (body.name.trim() === '') throw new Error('Enter a name');
      // `by` is the signed-in user the change is recorded against.
      const sent = { ...body, by: user ?? '' };
      return editing
        ? putJson<Person>(`${base}/${editing.id}`, { ...sent, version })
        : postJson<Person>(base, sent);
    },
    onSuccess: async () => {
      // A grade or terms change can move the dues of the person's claims.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      if (!mounted.current) return;
      if (editing) {
        onRefused(undefined);
        setEditing(undefined);
      } else {
        setName('');
        setGrade('A');
        setGradeFrom('');
        setTerms('');
        setContact('');
      }
    },
    onError: async (error) => {
      if (!editing || !mounted.current) return;
      if (!(error instanceof HttpError) || error.status !== 409) {
        onRefused(error.message);
        setEditing(undefined);
        // Re-read the people, so the next editor opened loads the person's current version.
        await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
        return;
      }
      // Someone else saved this person: re-read the sheet's people and show their current values.
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      const now = queryClient.getQueryData<Person[]>(peopleKey(sheetId))?.find((p) => p.id === editing.id);
      if (now) {
        setName(now.name);
        setGrade(now.grade);
        setTerms(now.terms == null ? '' : String(now.terms));
        setContact(now.contact ?? '');
        setVersion(now.version);
      }
    },
  });

  const submit = useSubmitOnce(save);
  const carried = editing ? undefined : refused;
  const reset = () => {
    save.reset();
    setTaken(undefined);
    onRefused(undefined);
  };
  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        onRefused(undefined);
        save.reset();
        const key = contact.trim().toLowerCase();
        if (key !== '' && (people.data ?? []).some((p) => p.id !== editing?.id && p.contact === key)) {
          setTaken(CONTACT_TAKEN);
          return;
        }
        setTaken(undefined);
        submit({ name, grade, gradeFrom, terms, contact });
      }}
    >
      <label>
        Name{' '}
        <input
          data-testid="person-form-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            reset();
          }}
        />
      </label>{' '}
      <label>
        Grade{' '}
        <select
          data-testid="person-form-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            reset();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        from{' '}
        <input
          data-testid="person-form-grade-from"
          type="date"
          value={gradeFrom}
          onChange={(e) => {
            setGradeFrom(e.target.value);
            reset();
          }}
        />
      </label>{' '}
      <label>
        Terms (days){' '}
        <input
          data-testid="person-form-terms"
          inputMode="numeric"
          value={terms}
          onChange={(e) => {
            setTerms(e.target.value);
            reset();
          }}
        />
      </label>{' '}
      <label>
        Contact{' '}
        <input
          data-testid="person-form-contact"
          value={contact}
          onChange={(e) => {
            setContact(e.target.value);
            reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
        {editing ? 'Save' : 'Add person'}
      </button>
      {editing && (
        <>
          {' '}
          <button type="button" data-testid="person-form-cancel" onClick={() => { onRefused(undefined); setEditing(undefined); }}>
            Cancel
          </button>
        </>
      )}
      {(save.isError || taken !== undefined || carried !== undefined) && (
        <p data-testid="person-form-error" role="alert">
          {taken ?? save.error?.message ?? carried}
        </p>
      )}
    </form>
  );
}

// The quick-add bar: a name and an amount make a draft claim dated the sheet's today, with an empty
// note, for the person of that name (case and surrounding spaces ignored) — adding them, grade A,
// when the name is not on the sheet. The server checks it all as one change, so a refusal adds no one.
function QuickAddBar({ sheetId }: { sheetId: string }) {
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (body: { name: string; amount: string }) => {
      if (body.name.trim() === '') throw new Error('Enter a name');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick-add`, { ...body, by: user ?? '' });
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
      setName('');
      setAmount('');
    },
  });

  const submit = useSubmitOnce(add);
  return (
    <form
      data-testid="quick-add"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ name, amount });
      }}
    >
      <label>
        Quick add: name{' '}
        <input
          data-testid="quick-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />
      </label>{' '}
      <label>
        Amount{' '}
        <input
          data-testid="quick-amount"
          inputMode="decimal"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            add.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="quick-submit" disabled={add.isPending}>
        Add draft claim
      </button>
      {add.isError && (
        <p data-testid="quick-error" role="alert">
          {add.error.message}
        </p>
      )}
    </form>
  );
}

/** Whether retired people are listed (search param `showRetired`): off when the screen is first opened, and left
 *  as it is by loading a sheet. */
export function useShowRetired() {
  return useSearchParam('showRetired', asFlag);
}

// The people on the sheet, in list order (each row moves up or down), and the form that adds or edits them. Retired people
// have no row unless "show retired" is on.
function PeoplePanel({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const [showRetired, setShowRetired] = useShowRetired();
  const [editingId, setEditing] = useEditingPerson();
  const editing = people.data?.find((p) => p.id === editingId);
  // A refused edit's message, shown on the add form once the editor closes.
  const [refused, setRefused] = useState<string>();
  // Each Edit press opens a fresh editor, filled once the sheet's people are re-read from the store,
  // so it starts from the person's current values and version (`loading`: the press still re-reading).
  const queryClient = useQueryClient();
  const [opened, setOpened] = useState(0);
  const [loading, setLoading] = useState<number>();
  const openEditor = async (id: number) => {
    const press = opened + 1;
    setRefused(undefined);
    setOpened(press);
    setLoading(press);
    setEditing(id);
    await queryClient.refetchQueries({ queryKey: peopleKey(sheetId), exact: true }).catch(() => undefined);
    setLoading((now) => (now === press ? undefined : now));
  };

  return (
    <section data-testid="people-panel">
      <h2>People</h2>
      {editingId !== undefined && loading !== undefined ? (
        <p data-testid="person-form-loading">Loading…</p>
      ) : (
        <PersonForm key={editing ? `edit-${editing.id}-${opened}` : 'add'} sheetId={sheetId} editing={editing}
          refused={refused} onRefused={setRefused} />
      )}
      <QuickAddBar sheetId={sheetId} />
      {people.isError && (
        <p data-testid="people-error" role="alert">
          {people.error.message}
        </p>
      )}
      <label>
        <input
          type="checkbox"
          data-testid="show-retired"
          checked={showRetired}
          onChange={(e) => setShowRetired(e.target.checked || undefined)}
        />{' '}
        Show retired
      </label>
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <th>Terms</th>
            <th>Contact</th>
            <PersonHeads.Slot sheetId={sheetId} user={user} />
            <th />
          </tr>
        </thead>
        <tbody>
          {(people.data ?? []).filter((p) => showRetired || p.retiredFrom === null).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <td data-testid="person-terms">{p.terms ?? ''}</td>
              <td data-testid="person-contact">{p.contact ?? ''}</td>
              <PersonCells.Slot sheetId={sheetId} user={user} personId={p.id} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => void openEditor(p.id)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 10, Component: PeoplePanel });

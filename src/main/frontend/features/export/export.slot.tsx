import { useCallback, useEffect, useState } from 'react';
import { useIsFetching, useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { ReadNow } from '../../query/readNow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { live, useClaims } from '../claims/claims';
import { gradeOn, usePeople } from '../people/people';
import { periodOf, personPeriodCarried, personPeriodDue, usePeriods } from '../periods/periods';
import { personCutTotal, usePools } from '../pools/pools';
import { claimDue, personDue, claimRate, sum, useRates } from '../rates/rates';
import { useShareParts } from '../header/totals';
import { policyVersionOn, useCap, useFloor, useLate, usePolicyVersions } from '../settings/settings';
import { type Found, SheetPeople } from '../search/search.slot';
import { csvValue, download } from './csv';

/** The button that downloads the people as a CSV file: name, grade in force today, retired-from date (or nothing)
 *  and due-total as shown, one line per person in list order, retired people included. The file is made once the
 *  sheet's figures are no longer being re-read, so it holds what the screen shows: while an as-of date is set (see
 *  features/asof) every figure is read as the sheet stood then, so the file holds the as-of figures; while a preview date is set (see
 *  features/preview) every figure takes it as the sheet's today, so the file holds the preview figures. */
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  const pools = usePools(sheetId);
  const parts = useShareParts(sheetId);
  const fetching = useIsFetching({ queryKey: ['sheet', sheetId] });
  const [wanted, setWanted] = useState(false);

  useEffect(() => {
    if (!wanted || fetching > 0 || !people.data) return;
    setWanted(false);
    const lines = ['name,grade,retired-from,due-total'];
    for (const p of people.data) {
      const due = personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor, cap, personCutTotal(pools.data, p.id), parts(p.id));
      lines.push([p.name, p.grade, p.retiredFrom ?? '', due].map(csvValue).join(','));
    }
    download(`people-${sheetId}.csv`, lines);
  }, [wanted, fetching, people.data, claims.data, rates.data, late, floor, cap, pools.data, parts, sheetId]);

  return (
    <p>
      <button type="button" data-testid="export-people" onClick={() => setWanted(true)}>
        Export people (CSV)
      </button>
    </p>
  );
}

/** The button that downloads the claims as a CSV file: person, number, date, amount, due, status and period as the
 *  claim rows show them (an empty number or period, or an n/a due, as it is), people in list order (retired people
 *  included) and each person's claims oldest first. Made once the sheet's figures are no longer being re-read; while an
 *  as-of date is set they are the as-of figures (only the claims that existed then, their due as it stood then); while a
 *  preview date is set they are the preview figures (each due as if the sheet's today were the preview date). */
function ExportClaims({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const periods = usePeriods(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const fetching = useIsFetching({ queryKey: ['sheet', sheetId] });
  const [wanted, setWanted] = useState(false);

  useEffect(() => {
    if (!wanted || fetching > 0 || !people.data || !claims.data) return;
    setWanted(false);
    const lines = ['person,number,date,amount,due,status,period'];
    for (const p of people.data) {
      const mine = claims.data.filter((c) => c.personId === p.id).reverse();
      for (const c of mine) {
        const due = claimDue(c, claimRate(rates.data, gradeOn(p, c.date), c), floor, late);
        const period = periodOf(periods.data, c.dueBy)?.name ?? '';
        lines.push([p.name, c.number ?? '', c.date, c.amount, due, c.status, period].map(csvValue).join(','));
      }
    }
    download(`claims-${sheetId}.csv`, lines);
  }, [wanted, fetching, people.data, claims.data, rates.data, periods.data, late, floor, sheetId]);

  return (
    <p>
      <button type="button" data-testid="export-claims" onClick={() => setWanted(true)}>
        Export claims (CSV)
      </button>
    </p>
  );
}

/** The button that downloads the pools as a CSV file: pool, member, share and cut as the pool rows show them (a pool
 *  member as pool:<name>, an empty cut when none), pools in the order added and each pool's members in the order added.
 *  Made once the sheet's figures are no longer being re-read; while an as-of date is set they are the as-of figures, and
 *  while a preview date is set the preview figures. */
function ExportPools({ sheetId }: { sheetId: string }) {
  const pools = usePools(sheetId);
  const fetching = useIsFetching({ queryKey: ['sheet', sheetId] });
  const [wanted, setWanted] = useState(false);

  useEffect(() => {
    if (!wanted || fetching > 0 || !pools.data) return;
    setWanted(false);
    const lines = ['pool,member,share,cut'];
    for (const pool of pools.data)
      for (const m of pool.members)
        lines.push([pool.name, m.pool === null ? m.name : `pool:${m.pool}`, m.share.toFixed(2), m.cut ?? ''].map(csvValue).join(','));
    download(`pools-${sheetId}.csv`, lines);
  }, [wanted, fetching, pools.data, sheetId]);

  return (
    <p>
      <button type="button" data-testid="export-pools" onClick={() => setWanted(true)}>
        Export pools (CSV)
      </button>
    </p>
  );
}

/** The button that downloads the period figures as a CSV file: person, period, figure and carried as the person rows
 *  show them (see personPeriodDue, personPeriodCarried), one line per person and period, people in list order (retired
 *  people included) and periods in the order added. Made once the sheet's figures are no longer being re-read; while an
 *  as-of date is set they are the as-of figures, and while a preview date is set the preview figures. */
function ExportPeriods({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const fetching = useIsFetching({ queryKey: ['sheet', sheetId] });
  const [wanted, setWanted] = useState(false);

  useEffect(() => {
    if (!wanted || fetching > 0 || !people.data || !periods.data) return;
    setWanted(false);
    const lines = ['person,period,figure,carried'];
    for (const p of people.data)
      for (const period of periods.data) {
        const figure = personPeriodDue(period, p, claims.data, rates.data, floor, late);
        const carried = personPeriodCarried(period, periods.data, p, claims.data, rates.data, floor, late);
        lines.push([p.name, period.name, figure, carried].map(csvValue).join(','));
      }
    download(`periods-${sheetId}.csv`, lines);
  }, [wanted, fetching, people.data, periods.data, claims.data, rates.data, late, floor, sheetId]);

  return (
    <p>
      <button type="button" data-testid="export-periods" onClick={() => setWanted(true)}>
        Export period figures (CSV)
      </button>
    </p>
  );
}

/** The button that downloads the people of the whole tree as a CSV file: sheet, name, grade and due-total as each
 *  sheet's person rows show them, this sheet's people first in list order, then the other sheets' (see the tree search)
 *  by sheet id. The sheets are read as they are now, afresh on each press. */
function ExportTree({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [found, setFound] = useState<Record<string, Found>>({});
  const [made, setMade] = useState(false);
  const read = useMutation({
    mutationFn: async () => {
      const others = await getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/tree`);
      const sheets = [sheetId, ...others];
      // re-read every sheet so what another user changed is seen
      await Promise.all(sheets.map((id) => queryClient.refetchQueries({ queryKey: ['sheet', id] })));
      return sheets;
    },
  });
  const report = useCallback((id: string, f: Found) => setFound((all) => ({ ...all, [id]: f })), []);
  const sheets = read.data;
  const done = sheets !== undefined && sheets.every((id) => found[id] !== undefined);

  useEffect(() => {
    if (!done || made) return;
    setMade(true);
    const lines = ['sheet,name,grade,due-total'];
    for (const id of sheets!) for (const p of found[id]) lines.push([p.sheetId, p.plainName, p.grade, p.due].map(csvValue).join(','));
    download(`tree-${sheetId}.csv`, lines);
  }, [done, made, sheets, found, sheetId]);

  return (
    <p>
      <button
        type="button"
        data-testid="export-tree"
        disabled={read.isPending}
        onClick={() => {
          read.reset();
          setFound({});
          setMade(false);
          read.mutate();
        }}
      >
        Export the tree's people (CSV)
      </button>
      {read.isError && <span data-testid="export-tree-error"> Could not read the tree.</span>}
      {sheets !== undefined && !made && (
        <ReadNow.Provider value={true}>
          {sheets.map((id) => (
            <SheetPeople key={id} sheetId={id} report={report} />
          ))}
        </ReadNow.Provider>
      )}
    </p>
  );
}

/** The button that downloads the policy versions as a CSV file: version, from (the date it starts), key, value (empty
 *  when blank), then its claims — the claims that count in the live totals whose date falls in it (see policyVersionOn)
 *  — counted, and their dues as the claim rows show them added up; one line per version in the order made. Made once
 *  the sheet's figures are no longer being re-read; while an as-of or a preview date is set they are those figures. */
function ExportPolicy({ sheetId }: { sheetId: string }) {
  const versions = usePolicyVersions(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const fetching = useIsFetching({ queryKey: ['sheet', sheetId] });
  const [wanted, setWanted] = useState(false);

  useEffect(() => {
    if (!wanted || fetching > 0 || !versions.data || !people.data || !claims.data) return;
    setWanted(false);
    const byId = new Map(people.data.map((p) => [p.id, p]));
    const counted = live(claims.data, people.data, late.today);
    const lines = ['version,from,key,value,claims,due-total'];
    for (const v of versions.data) {
      const mine = counted.filter((c) => policyVersionOn(versions.data, c.date) === v.number);
      const dues = mine.map((c) => {
        const p = byId.get(c.personId);
        return claimDue(c, p === undefined ? undefined : claimRate(rates.data, gradeOn(p, c.date), c), floor, late);
      });
      lines.push([String(v.number), v.start, v.key, v.value ?? '', String(mine.length), sum(dues)].map(csvValue).join(','));
    }
    download(`policy-${sheetId}.csv`, lines);
  }, [wanted, fetching, versions.data, people.data, claims.data, rates.data, late, floor, sheetId]);

  return (
    <p>
      <button type="button" data-testid="export-policy" onClick={() => setWanted(true)}>
        Export policy versions (CSV)
      </button>
    </p>
  );
}

export const contributions = [
  SheetPage.fill({ order: 15, Component: ExportPeople }),
  SheetPage.fill({ order: 15.5, Component: ExportClaims }),
  SheetPage.fill({ order: 15.6, Component: ExportPools }),
  SheetPage.fill({ order: 15.7, Component: ExportPeriods }),
  SheetPage.fill({ order: 15.8, Component: ExportTree }),
  SheetPage.fill({ order: 15.9, Component: ExportPolicy }),
];

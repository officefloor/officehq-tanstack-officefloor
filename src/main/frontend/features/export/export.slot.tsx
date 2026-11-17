import { useEffect, useState } from 'react';
import { useIsFetching } from '@tanstack/react-query';
import { SheetPage } from '../../slots/defs/sheetPage';
import { live, useClaims } from '../claims/claims';
import { gradeOn, usePeople } from '../people/people';
import { periodOf, usePeriods } from '../periods/periods';
import { personCutTotal, usePools } from '../pools/pools';
import { claimDue, personDue, claimRate, useRates } from '../rates/rates';
import { useShareParts } from '../header/totals';
import { useCap, useFloor, useLate } from '../settings/settings';
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

export const contributions = [
  SheetPage.fill({ order: 15, Component: ExportPeople }),
  SheetPage.fill({ order: 15.5, Component: ExportClaims }),
  SheetPage.fill({ order: 15.6, Component: ExportPools }),
];

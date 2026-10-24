import { useQuery } from '@tanstack/react-query';
import { getJsonLive } from '../../api/http';
import { useCurrentUser } from '../session/session';

// The signed-in user's locale: how the screen shows them its figures. Kept per user (not per sheet) in the store;
// a user with none saved is en. It changes only what the screen shows: entries are typed with a dot, and exports
// and audit lines keep the dot, under any locale.
export const LOCALES = ['en', 'de'] as const;
export type Locale = (typeof LOCALES)[number];

export type UserLocale = { user: string; locale: Locale };

export const localeQueryKey = (user: string) => ['user', user, 'locale'] as const;
export const localePath = (user: string) => `/api/users/${encodeURIComponent(user)}/locale`;

export function useUserLocale() {
  const [user] = useCurrentUser();
  return useQuery({
    queryKey: localeQueryKey(user ?? ''),
    queryFn: () => getJsonLive<UserLocale>(localePath(user ?? '')),
    enabled: !!user,
  });
}

// A figure: a cell whose whole text is a number with two decimals (an amount, a due figure, a total, a rate, a
// share, a cut), as every feature renders it — 1234.56.
const FIGURE = /^(\s*)(-?)(\d+)\.(\d{2})(\s*)$/;

/** The figure as the locale shows it (de: 1.234,56); any other text as it is. */
export function showFigure(text: string, locale: Locale): string {
  const m = locale === 'de' ? FIGURE.exec(text) : null;
  if (!m) return text;
  const [, lead, sign, whole, cents, trail] = m;
  return `${lead}${sign}${whole.replace(/\B(?=(\d{3})+$)/g, '.')},${cents}${trail}`;
}

// Text a person types or picks keeps its own form.
const KEEP = new Set(['OPTION', 'TEXTAREA', 'SCRIPT', 'STYLE']);

/**
 * Shows every figure under `root` in the locale, as features render and re-render them: features render figures
 * as plain dotted text, and this rewrites each figure's text node in place (remembering what was rendered, so a
 * switch back to en restores it). Returns the function that stops it and restores every figure.
 */
export function showFiguresIn(root: Node, locale: Locale): () => void {
  const rendered = new Map<Text, { raw: string; shown: string }>();

  const show = (node: Text) => {
    if (node.parentElement && KEEP.has(node.parentElement.tagName)) return;
    const now = node.nodeValue ?? '';
    const seen = rendered.get(node);
    const raw = seen && seen.shown === now ? seen.raw : now;
    const shown = showFigure(raw, locale);
    if (shown === raw) {
      rendered.delete(node);
    } else {
      rendered.set(node, { raw, shown });
    }
    if (shown !== now) node.nodeValue = shown;
  };
  const showAll = (node: Node) => {
    if (node.nodeType === Node.TEXT_NODE) return show(node as Text);
    const walker = document.createTreeWalker(node, NodeFilter.SHOW_TEXT);
    for (let t = walker.nextNode(); t; t = walker.nextNode()) show(t as Text);
  };

  const observer = new MutationObserver((records) => {
    for (const r of records) {
      if (r.type === 'characterData') show(r.target as Text);
      else r.addedNodes.forEach(showAll);
    }
    for (const node of rendered.keys()) if (!node.isConnected) rendered.delete(node);
  });
  showAll(root);
  observer.observe(root, { subtree: true, childList: true, characterData: true });
  return () => {
    observer.disconnect();
    for (const [node, { raw, shown }] of rendered) if (node.nodeValue === shown) node.nodeValue = raw;
    rendered.clear();
  };
}

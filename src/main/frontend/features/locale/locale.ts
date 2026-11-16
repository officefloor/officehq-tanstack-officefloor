import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** How the screen shows a user figures: en as stored (1234.56), de as 1.234,56. Kept per user, on every sheet. */
export const LOCALES = ['en', 'de'] as const;
export type Locale = (typeof LOCALES)[number];

export type UserLocale = { user: string; locale: Locale };

/** Not under ['sheet', …]: the locale belongs to the user, not to a sheet. */
export const localeKey = (user: string) => ['user', user, 'locale'];

/** The signed-in user's locale as stored (en when never chosen). */
export function useUserLocale(user: string) {
  return useQuery({
    queryKey: localeKey(user),
    queryFn: () => getJson<UserLocale>(`/api/users/${encodeURIComponent(user)}/locale`),
    enabled: user !== '',
  });
}

/** A figure: the whole text is a number with two decimals, as the store and every export write it. */
const FIGURE = /^(-?)(\d+)\.(\d{2})$/;

/** The figure as the locale shows it; anything that is not a figure as it is. Entries, exports and audit lines keep
 *  the stored form — this is for the screen only. */
export function formatFigure(text: string, locale: Locale): string {
  const m = FIGURE.exec(text);
  if (!m || locale === 'en') return text;
  return `${m[1]}${m[2].replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${m[3]}`;
}

// Figures are computed and rendered by every feature as stored strings; rather than every cell knowing the locale,
// the screen's text is shown in it here. A text node that is the whole text of its element and is a figure is
// rewritten in place; its stored form is remembered so it can be put back when the locale returns to en. React
// writes a cell's text only when its value changes, so a rewritten cell stays rewritten until then, and the new value
// is rewritten as it lands.
const SKIP = new Set(['OPTION', 'TEXTAREA', 'SCRIPT', 'STYLE']);
const stored = new WeakMap<Text, { raw: string; shown: string }>();

function showNode(node: Text, locale: Locale) {
  const parent = node.parentElement;
  if (!parent || SKIP.has(parent.tagName)) return;
  const kept = stored.get(node);
  // A value React wrote since (not what we showed) is the stored form now.
  const raw = kept && kept.shown === node.data ? kept.raw : node.data;
  const trimmed = raw.trim();
  if (!FIGURE.test(trimmed) || (parent.textContent ?? '').trim() !== node.data.trim()) {
    stored.delete(node);
    return;
  }
  const shown = raw.replace(trimmed, formatFigure(trimmed, locale));
  stored.set(node, { raw, shown });
  if (node.data !== shown) node.data = shown;
}

function showTree(root: Node, locale: Locale) {
  if (root.nodeType === Node.TEXT_NODE) return showNode(root as Text, locale);
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  for (let n = walker.nextNode(); n; n = walker.nextNode()) showNode(n as Text, locale);
}

/** Shows every figure under `root` in the locale, and keeps doing so as the screen changes, until the returned stop. */
export function showFiguresIn(root: Node, locale: Locale): () => void {
  showTree(root, locale);
  // Under en every figure is shown as stored: putting back the ones rewritten before is all there is to do.
  if (locale === 'en') return () => {};
  const observer = new MutationObserver((records) => {
    for (const r of records) {
      if (r.type === 'characterData') showNode(r.target as Text, locale);
      else r.addedNodes.forEach((n) => showTree(n, locale));
    }
  });
  observer.observe(root, { subtree: true, childList: true, characterData: true });
  return () => observer.disconnect();
}

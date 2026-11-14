import type { Locale } from './locale';
import { showFigure } from './locale';

// Shows every figure on the screen in the user's locale. Features render figures as they always have
// (1234.56); this rewrites, in place, each text that is the whole text of its element and a figure,
// and keeps doing so as the screen changes. Form fields are left alone: entries are typed with a dot.
const SKIP = new Set(['OPTION', 'TEXTAREA', 'SCRIPT', 'STYLE']);

/** The rendered (en) text of each text node this rewrote, to put back when the locale turns en. */
const rendered = new WeakMap<Text, string>();

function show(node: Text, locale: Locale) {
  const parent = node.parentElement;
  if (!parent || SKIP.has(parent.tagName)) return;
  const original = rendered.get(node);
  if (original !== undefined && node.data === showFigure(original.trim(), 'de')) {
    // Still as this rewrote it: kept under de, put back under en.
    if (locale === 'en') {
      rendered.delete(node);
      node.data = original;
    }
    return;
  }
  rendered.delete(node);
  if (locale !== 'de') return;
  const trimmed = node.data.trim();
  if ((parent.textContent ?? '').trim() !== trimmed) return;
  const shown = showFigure(trimmed, locale);
  if (shown === trimmed) return;
  rendered.set(node, node.data);
  node.data = shown;
}

function showAll(root: Node, locale: Locale) {
  if (root.nodeType === Node.TEXT_NODE) {
    show(root as Text, locale);
    return;
  }
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  for (let n = walker.nextNode(); n; n = walker.nextNode()) show(n as Text, locale);
}

/** Keeps every figure under `root` shown in `locale` until the returned stop is called. */
export function showFigures(root: HTMLElement, locale: Locale): () => void {
  showAll(root, locale);
  if (locale !== 'de') return () => {};
  const observer = new MutationObserver((records) => {
    for (const r of records) {
      if (r.type === 'characterData') show(r.target as Text, locale);
      else {
        r.addedNodes.forEach((n) => showAll(n, locale));
        // A child removed or added can make the rest of the element's text a whole figure, or not.
        if (r.target.nodeType === Node.ELEMENT_NODE) {
          r.target.childNodes.forEach((n) => n.nodeType === Node.TEXT_NODE && show(n as Text, locale));
        }
      }
    }
  });
  observer.observe(root, { subtree: true, childList: true, characterData: true });
  return () => observer.disconnect();
}

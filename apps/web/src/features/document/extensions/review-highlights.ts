import { Extension, type Editor } from "@tiptap/react";
import { Plugin, PluginKey } from "@tiptap/pm/state";
import { Decoration, DecorationSet } from "@tiptap/pm/view";

export const reviewHighlightsKey = new PluginKey("reviewHighlights");

export interface AnchorRange {
  from: number;
  to: number;
}

export interface ReviewHighlightsOptions {
  /** Called on each decoration pass to get the current anchored-comment ranges (ProseMirror positions). */
  getRanges: () => AnchorRange[];
}

/**
 * Highlights review-comment anchor ranges in the editor (Phase 6, FR-SUP-4). Ranges are supplied by
 * a callback so React can keep them current; call `refreshReviewHighlights(editor)` after the data
 * changes to force a redraw. Out-of-bounds ranges (from doc edits since the comment) are skipped.
 */
export const ReviewHighlights = Extension.create<ReviewHighlightsOptions>({
  name: "reviewHighlights",

  addOptions() {
    return { getRanges: () => [] };
  },

  addProseMirrorPlugins() {
    const getRanges = this.options.getRanges;
    return [
      new Plugin({
        key: reviewHighlightsKey,
        props: {
          decorations(state) {
            const size = state.doc.content.size;
            const decos = (getRanges() || [])
              .filter((r: AnchorRange) => r && r.from >= 0 && r.to <= size && r.from < r.to)
              .map((r: AnchorRange) => Decoration.inline(r.from, r.to, { class: "review-anchor" }));
            return DecorationSet.create(state.doc, decos);
          },
        },
      }),
    ];
  },
});

/** Force the highlight plugin to recompute after the anchored-comment set changes. */
export function refreshReviewHighlights(editor: Editor | null) {
  if (!editor) return;
  editor.view.dispatch(editor.state.tr.setMeta(reviewHighlightsKey, Date.now()));
}

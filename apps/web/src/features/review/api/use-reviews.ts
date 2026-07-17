"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";

const reviewsKey = (docId: string) => ["reviews", docId] as const;

export function useReviews(documentId: string) {
  return useQuery({
    queryKey: reviewsKey(documentId),
    queryFn: () => api.listReviews(documentId),
    enabled: !!documentId,
  });
}

export function useReviewInbox() {
  return useQuery({ queryKey: ["review-inbox"], queryFn: api.reviewInbox });
}

function useReviewMutation<TArgs>(documentId: string, fn: (a: TArgs) => Promise<unknown>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: reviewsKey(documentId) });
      qc.invalidateQueries({ queryKey: ["review-inbox"] });
    },
  });
}

export function useSubmitReview(documentId: string) {
  return useReviewMutation(documentId, (b: {
    documentId: string;
    documentSectionId?: string;
    reviewerUserId: string;
    note?: string;
  }) => api.submitReview(b));
}

export function useAddComment(documentId: string) {
  return useReviewMutation(documentId, (v: {
    reviewId: string;
    body: string;
    anchorStart?: number;
    anchorEnd?: number;
    quote?: string;
  }) => api.addReviewComment(v.reviewId, v));
}

export function useResolveComment(documentId: string) {
  return useReviewMutation(documentId, (v: { commentId: string; resolved: boolean }) =>
    api.resolveReviewComment(v.commentId, v.resolved),
  );
}

export function useDecide(documentId: string) {
  return useReviewMutation(documentId, (v: { reviewId: string; decision: string; summary?: string }) =>
    api.decideReview(v.reviewId, { decision: v.decision, summary: v.summary }),
  );
}

export function useResubmit(documentId: string) {
  return useReviewMutation(documentId, (v: { reviewId: string; note?: string }) =>
    api.resubmitReview(v.reviewId, v.note),
  );
}

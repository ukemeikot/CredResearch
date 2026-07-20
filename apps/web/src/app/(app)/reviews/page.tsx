import { ReviewInbox } from "@/features/review/components/review-inbox";

export default function ReviewsPage() {
  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-2xl font-bold text-slate-900">Reviews</h1>
      <p className="mt-1 text-sm text-slate-500">Your pending review requests.</p>
      <div className="mt-6">
        <ReviewInbox />
      </div>
    </div>
  );
}

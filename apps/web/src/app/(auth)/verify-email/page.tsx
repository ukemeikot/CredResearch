import { Suspense } from "react";
import { VerifyEmailScreen } from "@/features/auth/components/verify-email-screen";

// useSearchParams requires a Suspense boundary in the App Router.
export default function VerifyEmailPage() {
  return (
    <Suspense>
      <VerifyEmailScreen />
    </Suspense>
  );
}

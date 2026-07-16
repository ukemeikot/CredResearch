import { Suspense } from "react";
import { ResetPasswordScreen } from "@/features/auth/components/reset-password-screen";

// useSearchParams requires a Suspense boundary in the App Router.
export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordScreen />
    </Suspense>
  );
}

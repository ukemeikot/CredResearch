import { Suspense } from "react";
import { RegisterScreen } from "@/features/auth/components/register-screen";

// useSearchParams (?next=) requires a Suspense boundary in the App Router.
export default function RegisterPage() {
  return (
    <Suspense>
      <RegisterScreen />
    </Suspense>
  );
}

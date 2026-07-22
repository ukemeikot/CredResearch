import { Suspense } from "react";
import { LoginScreen } from "@/features/auth/components/login-screen";

// useSearchParams (?next=) requires a Suspense boundary in the App Router.
export default function LoginPage() {
  return (
    <Suspense>
      <LoginScreen />
    </Suspense>
  );
}

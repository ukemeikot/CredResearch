import { Suspense } from "react";
import { AcceptInviteScreen } from "@/features/project/components/accept-invite-screen";

// useSearchParams requires a Suspense boundary in the App Router.
export default function AcceptInvitePage() {
  return (
    <Suspense>
      <AcceptInviteScreen />
    </Suspense>
  );
}

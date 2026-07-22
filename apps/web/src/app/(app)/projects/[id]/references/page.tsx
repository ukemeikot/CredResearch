import { ReferencesPanel } from "@/features/paper/components/references-panel";

export default async function ReferencesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ReferencesPanel projectId={id} />;
}

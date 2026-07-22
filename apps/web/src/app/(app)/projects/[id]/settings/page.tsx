import { ProjectSettingsSection } from "@/features/project/components/project-settings-section";

export default async function SettingsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ProjectSettingsSection id={id} />;
}

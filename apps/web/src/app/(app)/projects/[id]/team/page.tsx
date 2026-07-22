import { ProjectTeamSection } from "@/features/project/components/project-team-section";

export default async function TeamPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ProjectTeamSection id={id} />;
}

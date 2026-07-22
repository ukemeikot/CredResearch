import { ProjectAnalysisSection } from "@/features/project/components/project-simple-sections";

export default async function AnalysisPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ProjectAnalysisSection id={id} />;
}

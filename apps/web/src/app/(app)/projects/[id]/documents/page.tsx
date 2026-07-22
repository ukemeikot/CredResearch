import { ProjectDocumentsSection } from "@/features/project/components/project-simple-sections";

export default async function DocumentsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ProjectDocumentsSection id={id} />;
}

import { QuestionnairesPanel } from "@/features/questionnaire/components/questionnaires-panel";

export default async function QuestionnairesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <QuestionnairesPanel projectId={id} />;
}

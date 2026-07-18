import { QuestionnaireBuilder } from "@/features/questionnaire/components/questionnaire-builder";

export default async function Page({ params }: { params: Promise<{ id: string; qid: string }> }) {
  const { id, qid } = await params;
  return <QuestionnaireBuilder projectId={id} id={qid} />;
}

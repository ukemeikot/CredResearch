import { DocumentEditor } from "@/features/document/components/document-editor";

export default async function DocumentPage({ params }: { params: Promise<{ docId: string }> }) {
  const { docId } = await params;
  return <DocumentEditor docId={docId} />;
}

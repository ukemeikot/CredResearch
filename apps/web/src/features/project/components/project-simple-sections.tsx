"use client";

import { DataAnalysisPanel } from "@/features/analysis/components/data-analysis-panel";
import { DocumentsPanel } from "@/features/document/components/documents-panel";
import { useProjectRole } from "../api/use-project-role";
import { useProject } from "../api/use-projects";

export function ProjectDocumentsSection({ id }: { id: string }) {
  const { isOwner } = useProjectRole(id);
  return <DocumentsPanel projectId={id} canCreate={isOwner} />;
}

export function ProjectAnalysisSection({ id }: { id: string }) {
  const { data } = useProject(id);
  return <DataAnalysisPanel projectId={id} projectTitle={data?.project.title ?? ""} />;
}

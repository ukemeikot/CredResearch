import { z } from "zod";

/**
 * Zod schemas for API responses — the single source of truth for the client's data shapes.
 * TypeScript types are inferred from these (`z.infer`), and responses are validated at the
 * fetch boundary (see `lib/api.ts`) so malformed/unexpected payloads fail loudly and close to
 * the network edge rather than surfacing as confusing UI bugs later.
 */

export const ProjectStatusSchema = z.enum([
  "DRAFT",
  "PROPOSAL",
  "IN_PROGRESS",
  "UNDER_REVIEW",
  "REVISIONS",
  "APPROVED",
  "COMPLETED",
]);
export type ProjectStatus = z.infer<typeof ProjectStatusSchema>;

export const ProjectMemberRoleSchema = z.enum(["OWNER", "SUPERVISOR", "CONSULTANT", "VIEWER"]);
export type ProjectMemberRole = z.infer<typeof ProjectMemberRoleSchema>;

const Level = z.enum(["LOW", "MEDIUM", "HIGH"]);

export const TokenResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  expiresIn: z.number(),
  user: z.object({
    id: z.string(),
    roles: z.array(z.string()),
    institutionId: z.string(),
    plan: z.string(),
  }),
});
export type TokenResponse = z.infer<typeof TokenResponseSchema>;

export const ProfileSchema = z.object({
  id: z.string(),
  email: z.string(),
  fullName: z.string(),
  institutionId: z.string(),
  departmentId: z.string().nullable(),
  academicLevel: z.string().nullable(),
  fieldOfStudy: z.string().nullable(),
  orcid: z.string().nullable(),
  emailVerified: z.boolean(),
  roles: z.array(z.string()),
});
export type Profile = z.infer<typeof ProfileSchema>;

export const ProjectSummarySchema = z.object({
  id: z.string(),
  institutionId: z.string(),
  departmentId: z.string().nullable(),
  ownerUserId: z.string(),
  title: z.string(),
  level: z.string().nullable(),
  status: ProjectStatusSchema,
  abstractText: z.string().nullable(),
});
export type ProjectSummary = z.infer<typeof ProjectSummarySchema>;

export const MilestoneSchema = z.object({
  id: z.string(),
  projectId: z.string(),
  title: z.string(),
  dueDate: z.string().nullable(),
  status: z.string().nullable(),
  completedAt: z.string().nullable(),
});
export type Milestone = z.infer<typeof MilestoneSchema>;

export const ProjectMemberSchema = z.object({
  id: z.string(),
  projectId: z.string(),
  userId: z.string(),
  role: ProjectMemberRoleSchema,
});
export type ProjectMember = z.infer<typeof ProjectMemberSchema>;

export const ActivitySchema = z.object({
  id: z.string(),
  projectId: z.string(),
  actorUserId: z.string(),
  type: z.string(),
  payload: z.string().nullable(),
  createdAt: z.string(),
});
export type Activity = z.infer<typeof ActivitySchema>;

export const DashboardSchema = z.object({
  status: ProjectStatusSchema,
  totalMilestones: z.number(),
  completedMilestones: z.number(),
  nextMilestone: MilestoneSchema.nullable(),
  memberCount: z.number(),
});
export type Dashboard = z.infer<typeof DashboardSchema>;

export const ProjectDetailSchema = z.object({
  project: ProjectSummarySchema,
  dashboard: DashboardSchema,
  members: z.array(ProjectMemberSchema),
  milestones: z.array(MilestoneSchema),
});
export type ProjectDetail = z.infer<typeof ProjectDetailSchema>;

export const InstitutionSchema = z.object({
  id: z.string(),
  name: z.string(),
  country: z.string().nullable(),
  type: z.string().nullable(),
  personalTenant: z.boolean(),
  status: z.string(),
});
export type Institution = z.infer<typeof InstitutionSchema>;

export const DepartmentSchema = z.object({
  id: z.string(),
  institutionId: z.string(),
  name: z.string(),
  code: z.string().nullable(),
});
export type Department = z.infer<typeof DepartmentSchema>;

export const TemplateSchema = z.object({
  id: z.string(),
  name: z.string(),
  level: z.string().nullable(),
  global: z.boolean(),
  citationStyle: z.string(),
});
export type Template = z.infer<typeof TemplateSchema>;

export const TemplateSectionSchema = z.object({
  id: z.string(),
  orderIndex: z.number(),
  chapter: z.string().nullable(),
  heading: z.string(),
  guidance: z.string().nullable(),
});
export type TemplateSection = z.infer<typeof TemplateSectionSchema>;

export const FormatRuleSchema = z.object({
  fontFamily: z.string(),
  fontSizePt: z.number(),
  lineSpacing: z.number(),
  margins: z.unknown(),
  headingNumbering: z.string(),
  citationStyle: z.string(),
});
export type FormatRule = z.infer<typeof FormatRuleSchema>;

export const TemplateDetailSchema = z.object({
  template: TemplateSchema,
  sections: z.array(TemplateSectionSchema),
  formatRule: FormatRuleSchema.nullable(),
});
export type TemplateDetail = z.infer<typeof TemplateDetailSchema>;

export const DocumentSummarySchema = z.object({
  id: z.string(),
  projectId: z.string(),
  templateId: z.string(),
  title: z.string(),
  status: z.string(),
});
export type DocumentSummary = z.infer<typeof DocumentSummarySchema>;

export const DocSectionSchema = z.object({
  id: z.string(),
  documentId: z.string(),
  orderIndex: z.number(),
  chapter: z.string().nullable(),
  heading: z.string(),
  content: z.unknown().nullable(),
  version: z.number(),
});
export type DocSection = z.infer<typeof DocSectionSchema>;

export const DocumentDetailSchema = z.object({
  document: DocumentSummarySchema,
  sections: z.array(DocSectionSchema),
});
export type DocumentDetail = z.infer<typeof DocumentDetailSchema>;

export const DocVersionSchema = z.object({
  id: z.string(),
  version: z.number(),
  authoredBy: z.string().nullable(),
  createdAt: z.string(),
});
export type DocVersion = z.infer<typeof DocVersionSchema>;

export const InvitationSchema = z.object({
  id: z.string(),
  email: z.string(),
  role: z.string(),
  status: z.string(),
  expiresAt: z.string(),
});
export type Invitation = z.infer<typeof InvitationSchema>;

// ── AI (Phase 4) ─────────────────────────────────────────────────────────────
export const AiTopicSchema = z.object({
  title: z.string(),
  rationale: z.string(),
  feasibility: Level,
  suggested_methods: z.array(z.string()),
});
export type AiTopic = z.infer<typeof AiTopicSchema>;

export const AiObjectivesSchema = z.object({
  aim: z.string(),
  objectives: z.array(z.string()),
  research_questions: z.array(z.string()),
  hypotheses: z.array(z.string()),
});
export type AiObjectives = z.infer<typeof AiObjectivesSchema>;

export const AiAlignmentFindingSchema = z.object({
  area: z.string(),
  issue: z.string(),
  suggestion: z.string(),
  severity: Level,
});
export type AiAlignmentFinding = z.infer<typeof AiAlignmentFindingSchema>;

export const AiAlignmentSchema = z.object({
  overall_score: z.number(),
  summary: z.string(),
  findings: z.array(AiAlignmentFindingSchema),
});
export type AiAlignment = z.infer<typeof AiAlignmentSchema>;

export const AiCreditsSchema = z.object({
  plan: z.string(),
  used: z.number(),
  limit: z.number(),
  remaining: z.number(),
});
export type AiCredits = z.infer<typeof AiCreditsSchema>;

export const DisclosureEntrySchema = z.object({
  id: z.string(),
  documentSectionId: z.string().nullable(),
  featureKey: z.string(),
  model: z.string().nullable(),
  suggestionSummary: z.string().nullable(),
  action: z.string(),
  entryHash: z.string(),
  createdAt: z.string(),
});
export type DisclosureEntry = z.infer<typeof DisclosureEntrySchema>;

// ── Papers / references (Phase 5) ────────────────────────────────────────────
export const PaperSummarySchema = z.object({
  summary: z.string().default(""),
  methodology: z.string().default(""),
  findings: z.array(z.string()).default([]),
  limitations: z.array(z.string()).default([]),
  gaps: z.array(z.string()).default([]),
});
export type PaperSummary = z.infer<typeof PaperSummarySchema>;

export const PaperSchema = z.object({
  id: z.string(),
  projectId: z.string(),
  filename: z.string().nullable(),
  title: z.string().nullable(),
  authors: z.string().nullable(),
  year: z.number().nullable(),
  doi: z.string().nullable(),
  journal: z.string().nullable(),
  extractionStatus: z.string(),
  summary: PaperSummarySchema.nullable().optional(),
});
export type Paper = z.infer<typeof PaperSchema>;

export const ReferenceSchema = z.object({ paperId: z.string(), text: z.string() });
export const ReferenceListSchema = z.object({
  style: z.string(),
  references: z.array(ReferenceSchema),
});
export type Reference = z.infer<typeof ReferenceSchema>;
export type ReferenceList = z.infer<typeof ReferenceListSchema>;

// ── Small ad-hoc response shapes ─────────────────────────────────────────────
export const MessageSchema = z.object({ message: z.string() });
export const RegisterResponseSchema = z.object({ userId: z.string(), message: z.string() });
export const OnboardInstitutionResponseSchema = z.object({ institutionId: z.string() });
export const AcceptInviteResponseSchema = z.object({ projectId: z.string() });
export const AiTopicsResponseSchema = z.object({ topics: z.array(AiTopicSchema) });
export const AiProblemStatementResponseSchema = z.object({
  problem_statement: z.string(),
  significance: z.string(),
});
export const AiSectionAssistResponseSchema = z.object({
  suggestion: z.string(),
  notes: z.array(z.string()),
});

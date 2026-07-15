package africa.credresearch.modules.project.domain;

/** Project-scoped role of a member (FR-PROJ-2/3). Distinct from platform RBAC roles. */
public enum ProjectMemberRole {
    OWNER,
    SUPERVISOR,
    CONSULTANT,
    VIEWER
}

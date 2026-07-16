package africa.credresearch.modules.document.domain.model;

import java.util.UUID;

/** A document template (global or tenant-owned). FR-TMPL-1/2/3. */
public record Template(
        UUID id,
        UUID institutionId,
        UUID departmentId,
        String name,
        String level,
        boolean global,
        String citationStyle) {}

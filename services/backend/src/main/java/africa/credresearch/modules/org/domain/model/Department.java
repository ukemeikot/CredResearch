package africa.credresearch.modules.org.domain.model;

import java.util.UUID;

public record Department(UUID id, UUID institutionId, String name, String code) {}

package africa.credresearch.modules.identity.application.dto;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID id, Set<String> roles, UUID institutionId, String plan) {}

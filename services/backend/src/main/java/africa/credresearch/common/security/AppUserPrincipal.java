package africa.credresearch.common.security;

import java.util.Set;
import java.util.UUID;

/** Authenticated principal carried in the SecurityContext, derived from the access token. */
public record AppUserPrincipal(UUID userId, UUID institutionId, Set<String> roles, String plan) {}

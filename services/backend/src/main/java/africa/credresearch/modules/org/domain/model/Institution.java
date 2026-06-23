package africa.credresearch.modules.org.domain.model;

import java.util.UUID;

public record Institution(
        UUID id,
        String name,
        String country,
        String type,
        boolean personalTenant,
        String status) {}

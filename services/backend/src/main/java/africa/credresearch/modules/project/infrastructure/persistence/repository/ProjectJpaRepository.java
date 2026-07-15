package africa.credresearch.modules.project.infrastructure.persistence.repository;

import africa.credresearch.modules.project.infrastructure.persistence.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {

    Optional<ProjectEntity> findByIdAndInstitutionIdAndDeletedAtIsNull(UUID id, UUID institutionId);

    List<ProjectEntity> findByInstitutionIdAndDeletedAtIsNull(UUID institutionId, Pageable pageable);

    @Query("""
            select p from ProjectEntity p
            where p.institutionId = :institutionId and p.deletedAt is null
              and exists (
                select 1 from ProjectMemberEntity m
                where m.projectId = p.id and m.userId = :userId and m.deletedAt is null)
            order by p.createdAt desc
            """)
    List<ProjectEntity> findByInstitutionAndMember(@Param("institutionId") UUID institutionId,
                                                   @Param("userId") UUID userId,
                                                   Pageable pageable);

    long countByInstitutionIdAndDeletedAtIsNull(UUID institutionId);
}

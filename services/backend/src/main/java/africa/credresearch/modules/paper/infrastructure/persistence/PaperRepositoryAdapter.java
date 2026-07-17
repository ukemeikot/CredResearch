package africa.credresearch.modules.paper.infrastructure.persistence;

import africa.credresearch.modules.paper.domain.model.Paper;
import africa.credresearch.modules.paper.domain.port.PaperRepository;
import africa.credresearch.modules.paper.infrastructure.persistence.entity.PaperEntity;
import africa.credresearch.modules.paper.infrastructure.persistence.repository.PaperJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaperRepositoryAdapter implements PaperRepository {

    private final PaperJpaRepository jpa;

    public PaperRepositoryAdapter(PaperJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Paper create(Paper p, String textContent) {
        PaperEntity e = new PaperEntity();
        e.setProjectId(p.projectId());
        e.setUploadedBy(p.uploadedBy());
        e.setFilename(p.filename());
        e.setTitle(p.title());
        e.setAuthors(p.authors());
        e.setYear(p.year());
        e.setDoi(p.doi());
        e.setJournal(p.journal());
        e.setExtractionStatus(p.extractionStatus());
        e.setTextContent(textContent);
        return toModel(jpa.save(e));
    }

    @Override
    public List<Paper> findByProject(UUID projectId) {
        return jpa.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(PaperRepositoryAdapter::toModel).toList();
    }

    @Override
    public Optional<Paper> findById(UUID id) {
        return jpa.findById(id).map(PaperRepositoryAdapter::toModel);
    }

    @Override
    public Paper updateMetadata(UUID id, String title, String authors, Integer year, String doi, String journal) {
        PaperEntity e = jpa.findById(id).orElseThrow();
        if (title != null) e.setTitle(title);
        if (authors != null) e.setAuthors(authors);
        if (year != null) e.setYear(year);
        if (doi != null) e.setDoi(doi);
        if (journal != null) e.setJournal(journal);
        e.setExtractionStatus("DONE"); // a user edit resolves any low-confidence flag
        return toModel(jpa.save(e));
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }

    private static Paper toModel(PaperEntity e) {
        return new Paper(e.getId(), e.getProjectId(), e.getUploadedBy(), e.getFilename(), e.getTitle(),
                e.getAuthors(), e.getYear(), e.getDoi(), e.getJournal(), e.getExtractionStatus(), e.getCreatedAt());
    }
}

package africa.credresearch.modules.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.modules.document.domain.model.Document;
import africa.credresearch.modules.document.domain.model.DocumentSection;
import africa.credresearch.modules.document.domain.port.DocumentRepository;
import africa.credresearch.modules.document.domain.port.DocumentSectionRepository;
import africa.credresearch.modules.document.domain.port.DocumentVersionRepository;
import africa.credresearch.modules.project.application.ProjectAccessGuard;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Autosave optimistic-locking behaviour (FR-DOC-3) with mocked ports. */
class DocumentServiceTest {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentSectionRepository sections = mock(DocumentSectionRepository.class);
    private final DocumentVersionRepository versions = mock(DocumentVersionRepository.class);
    private final TemplateService templates = mock(TemplateService.class);
    private final ProjectAccessGuard accessGuard = mock(ProjectAccessGuard.class);

    private final DocumentService service =
            new DocumentService(documents, sections, versions, templates, accessGuard);
    private final ObjectMapper mapper = new ObjectMapper();

    private final UUID docId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    private void asOwner() {
        TenantContextHolder.set(new TenantContext(UUID.randomUUID(), UUID.randomUUID(), Set.of("STUDENT"), "FREE"));
        when(documents.findById(docId)).thenReturn(Optional.of(new Document(docId, projectId, UUID.randomUUID(), "Doc", "DRAFT")));
        when(sections.findById(sectionId)).thenReturn(Optional.of(
                new DocumentSection(sectionId, docId, 0, "Ch1", "Intro", null, null, 3)));
    }

    @Test
    void autosaveSucceedsAndSnapshotsNewVersion() throws Exception {
        asOwner();
        when(sections.tryAutosave(eq(sectionId), eq(3), any(), any(), any())).thenReturn(Optional.of(
                new DocumentSection(sectionId, docId, 0, "Ch1", "Intro", "{}", "text", 4)));

        DocumentSection saved = service.autosave(docId, sectionId, mapper.readTree("{\"type\":\"doc\"}"), 3);

        assertThat(saved.version()).isEqualTo(4);
        verify(versions).snapshot(eq(sectionId), eq(4), any(), any(), any());
    }

    @Test
    void autosaveWithStaleVersionThrowsConflictAndDoesNotSnapshot() throws Exception {
        asOwner();
        when(sections.tryAutosave(eq(sectionId), eq(3), any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.autosave(docId, sectionId, mapper.readTree("{\"type\":\"doc\"}"), 3))
                .isInstanceOf(ApiException.class);
        verify(versions, never()).snapshot(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
        // Autosave is an owner-only write.
        verify(accessGuard).requireRole(projectId, ProjectMemberRole.OWNER);
    }
}

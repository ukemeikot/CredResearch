package africa.credresearch.modules.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** State-machine tests for the project lifecycle (FR-PROJ-4). */
class ProjectStatusTest {

    @Test
    void legalForwardTransitions_areAllowed() {
        assertThat(ProjectStatus.DRAFT.canTransitionTo(ProjectStatus.PROPOSAL)).isTrue();
        assertThat(ProjectStatus.PROPOSAL.canTransitionTo(ProjectStatus.IN_PROGRESS)).isTrue();
        assertThat(ProjectStatus.IN_PROGRESS.canTransitionTo(ProjectStatus.UNDER_REVIEW)).isTrue();
        assertThat(ProjectStatus.UNDER_REVIEW.canTransitionTo(ProjectStatus.REVISIONS)).isTrue();
        assertThat(ProjectStatus.UNDER_REVIEW.canTransitionTo(ProjectStatus.APPROVED)).isTrue();
        assertThat(ProjectStatus.APPROVED.canTransitionTo(ProjectStatus.COMPLETED)).isTrue();
    }

    @Test
    void revisionsAndUnderReview_mayBounce() {
        assertThat(ProjectStatus.REVISIONS.canTransitionTo(ProjectStatus.UNDER_REVIEW)).isTrue();
        assertThat(ProjectStatus.UNDER_REVIEW.canTransitionTo(ProjectStatus.REVISIONS)).isTrue();
    }

    @Test
    void illegalForwardJumps_areRejected() {
        assertThat(ProjectStatus.DRAFT.canTransitionTo(ProjectStatus.IN_PROGRESS)).isFalse();
        assertThat(ProjectStatus.DRAFT.canTransitionTo(ProjectStatus.COMPLETED)).isFalse();
        assertThat(ProjectStatus.PROPOSAL.canTransitionTo(ProjectStatus.APPROVED)).isFalse();
        assertThat(ProjectStatus.IN_PROGRESS.canTransitionTo(ProjectStatus.COMPLETED)).isFalse();
    }

    @Test
    void backwardTransitions_areRejected() {
        assertThat(ProjectStatus.PROPOSAL.canTransitionTo(ProjectStatus.DRAFT)).isFalse();
        assertThat(ProjectStatus.IN_PROGRESS.canTransitionTo(ProjectStatus.PROPOSAL)).isFalse();
        assertThat(ProjectStatus.APPROVED.canTransitionTo(ProjectStatus.REVISIONS)).isFalse();
    }

    @Test
    void completed_isTerminal() {
        for (ProjectStatus target : ProjectStatus.values()) {
            assertThat(ProjectStatus.COMPLETED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void sameStatusAndNull_areRejected() {
        assertThat(ProjectStatus.DRAFT.canTransitionTo(ProjectStatus.DRAFT)).isFalse();
        assertThat(ProjectStatus.DRAFT.canTransitionTo(null)).isFalse();
    }
}

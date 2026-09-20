package seungyong.helpmebackend.project.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.application.port.out.ProjectWorkPortOut;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;

@Repository
@RequiredArgsConstructor
public class ProjectWorkAdapter implements ProjectWorkPortOut {
    private final ProjectJpaRepository repository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockActive(Long projectId) {
        return repository.findByIdForUpdate(projectId)
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE).isPresent();
    }
}

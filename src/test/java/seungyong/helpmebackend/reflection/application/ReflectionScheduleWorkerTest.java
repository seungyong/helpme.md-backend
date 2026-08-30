package seungyong.helpmebackend.reflection.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.reflection.application.port.in.command.CreateReflectionCommand;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReflectionScheduleWorkerTest {
    @Mock private ProjectPortOut projectPortOut;
    @Mock private ReflectionService reflectionService;

    @Test
    @DisplayName("설정 timezone의 최근 도래한 일일·주간 기간을 각각 한 번 queue")
    void enqueue_latestDuePeriods() {
        ReflectionScheduleWorker worker =
                new ReflectionScheduleWorker(projectPortOut, reflectionService);
        Project project = Project.builder()
                .id(101L)
                .userId(1L)
                .repoFullName("octocat/helpme")
                .settings(ProjectSettings.defaults())
                .build();

        worker.enqueue(project, OffsetDateTime.parse("2026-08-30T15:00:00Z"));

        ArgumentCaptor<CreateReflectionCommand> captor =
                ArgumentCaptor.forClass(CreateReflectionCommand.class);
        verify(reflectionService, times(2)).createReflection(captor.capture());
        List<CreateReflectionCommand> commands = captor.getAllValues();
        assertThat(commands)
                .extracting(CreateReflectionCommand::kind)
                .containsExactly("daily", "weekly");
        assertThat(commands)
                .extracting(CreateReflectionCommand::periodStart)
                .containsExactly(
                        java.time.LocalDate.of(2026, 8, 30),
                        java.time.LocalDate.of(2026, 8, 24)
                );
    }
}

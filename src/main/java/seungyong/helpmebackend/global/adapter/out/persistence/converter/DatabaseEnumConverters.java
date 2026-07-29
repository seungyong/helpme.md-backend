package seungyong.helpmebackend.global.adapter.out.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;
import seungyong.helpmebackend.notion.domain.type.NotionConnectionStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportFormat;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioExportStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioStatus;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioTone;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.project.domain.type.ProjectWebhookStatus;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;
import seungyong.helpmebackend.user.domain.type.PlanCode;
import seungyong.helpmebackend.user.domain.type.UserStatus;
import seungyong.helpmebackend.webhook.domain.type.WebhookDeliveryStatus;

import java.util.Arrays;

public final class DatabaseEnumConverters {
    private DatabaseEnumConverters() {
    }

    public abstract static class DatabaseEnumConverter<E extends Enum<E> & DatabaseValueEnum>
            implements AttributeConverter<E, String> {
        private final Class<E> enumType;

        protected DatabaseEnumConverter(Class<E> enumType) {
            this.enumType = enumType;
        }

        @Override
        public String convertToDatabaseColumn(E attribute) {
            return attribute == null ? null : attribute.getDatabaseValue();
        }

        @Override
        public E convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }

            return Arrays.stream(enumType.getEnumConstants())
                    .filter(value -> value.getDatabaseValue().equals(dbData))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown " + enumType.getSimpleName() + " database value: " + dbData
                    ));
        }
    }

    @Converter
    public static class PlanCodeConverter extends DatabaseEnumConverter<PlanCode> {
        public PlanCodeConverter() {
            super(PlanCode.class);
        }
    }

    @Converter
    public static class UserStatusConverter extends DatabaseEnumConverter<UserStatus> {
        public UserStatusConverter() {
            super(UserStatus.class);
        }
    }

    @Converter
    public static class GithubTokenStatusConverter extends DatabaseEnumConverter<GithubTokenStatus> {
        public GithubTokenStatusConverter() {
            super(GithubTokenStatus.class);
        }
    }

    @Converter
    public static class ProjectStatusConverter extends DatabaseEnumConverter<ProjectStatus> {
        public ProjectStatusConverter() {
            super(ProjectStatus.class);
        }
    }

    @Converter
    public static class ProjectSyncStatusConverter extends DatabaseEnumConverter<ProjectSyncStatus> {
        public ProjectSyncStatusConverter() {
            super(ProjectSyncStatus.class);
        }
    }

    @Converter
    public static class ProjectWebhookStatusConverter extends DatabaseEnumConverter<ProjectWebhookStatus> {
        public ProjectWebhookStatusConverter() {
            super(ProjectWebhookStatus.class);
        }
    }

    @Converter
    public static class WebhookDeliveryStatusConverter extends DatabaseEnumConverter<WebhookDeliveryStatus> {
        public WebhookDeliveryStatusConverter() {
            super(WebhookDeliveryStatus.class);
        }
    }

    @Converter
    public static class ActivityTypeConverter extends DatabaseEnumConverter<ActivityType> {
        public ActivityTypeConverter() {
            super(ActivityType.class);
        }
    }

    @Converter
    public static class ReflectionKindConverter extends DatabaseEnumConverter<ReflectionKind> {
        public ReflectionKindConverter() {
            super(ReflectionKind.class);
        }
    }

    @Converter
    public static class ReflectionStatusConverter extends DatabaseEnumConverter<ReflectionStatus> {
        public ReflectionStatusConverter() {
            super(ReflectionStatus.class);
        }
    }

    @Converter
    public static class SourceQualityConverter extends DatabaseEnumConverter<SourceQuality> {
        public SourceQualityConverter() {
            super(SourceQuality.class);
        }
    }

    @Converter
    public static class PortfolioToneConverter extends DatabaseEnumConverter<PortfolioTone> {
        public PortfolioToneConverter() {
            super(PortfolioTone.class);
        }
    }

    @Converter
    public static class PortfolioStatusConverter extends DatabaseEnumConverter<PortfolioStatus> {
        public PortfolioStatusConverter() {
            super(PortfolioStatus.class);
        }
    }

    @Converter
    public static class PortfolioExportFormatConverter extends DatabaseEnumConverter<PortfolioExportFormat> {
        public PortfolioExportFormatConverter() {
            super(PortfolioExportFormat.class);
        }
    }

    @Converter
    public static class PortfolioExportStatusConverter extends DatabaseEnumConverter<PortfolioExportStatus> {
        public PortfolioExportStatusConverter() {
            super(PortfolioExportStatus.class);
        }
    }

    @Converter
    public static class PortfolioConflictActionConverter extends DatabaseEnumConverter<PortfolioConflictAction> {
        public PortfolioConflictActionConverter() {
            super(PortfolioConflictAction.class);
        }
    }

    @Converter
    public static class NotionConnectionStatusConverter extends DatabaseEnumConverter<NotionConnectionStatus> {
        public NotionConnectionStatusConverter() {
            super(NotionConnectionStatus.class);
        }
    }
}

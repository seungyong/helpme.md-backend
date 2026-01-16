package seungyong.helpmebackend.domain.entity.evaluation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import seungyong.helpmebackend.domain.vo.EvaluationStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class Evaluation {
    private Long id;
    private Long uploaderId;
    private String repoFullName;
    private Float rating;
    private String content;
    private EvaluationStatus status;
    private LocalDateTime updatedAt;

    /**
     * README가 없는 상태의 Evaluation 객체를 생성합니다.
     * @param uploaderId 평가를 업로드한 사용자 ID
     * @param repoFullName 평가 대상 저장소의 전체 이름
     * @return README 없음 상태의 Evaluation 객체
     */
    public static Evaluation createNoneStatusEvaluation(Long uploaderId, String repoFullName) {
        return new Evaluation(
                null,
                uploaderId,
                repoFullName,
                null,
                null,
                EvaluationStatus.NONE,
                null
        );
    }

    public static Evaluation createWithStatusEvaluation(
            Long uploaderId,
            String repoFullName,
            Float rating,
            String content,
            EvaluationStatus status
    ) {
        if (uploaderId == null) {
            throw new IllegalArgumentException("Uploader ID cannot be null.");
        } else if (status == EvaluationStatus.NONE) {
            throw new IllegalArgumentException("Status cannot be NONE for this method.");
        } else if (status == null) {
            throw new IllegalArgumentException("Status cannot be null.");
        } if (rating != null && (rating < 0.0f || rating > 5.0f)) {
            if (content == null) {
                throw new IllegalArgumentException("Content cannot be null when rating is provided.");
            }

            throw new IllegalArgumentException("Rating must be between 0.0 and 5.0.");
        }

        return new Evaluation(
                null,
                uploaderId,
                repoFullName,
                rating,
                content,
                status,
                null
        );
    }

    /**
     * 평가 상태를 NONE으로 변경하고, 내용과 평점을 초기화합니다.
     * @return 수정된 Evaluation 객체
     */
    public void changeNoneStatus() {
        this.status = EvaluationStatus.NONE;
        this.content = null;
        this.rating = null;
    }

    /**
     * 평가 상태를 CREATED로 변경합니다.
     */
    public void changeCreatedStatus() {
        this.status = EvaluationStatus.CREATED;
    }

    /**
     * 평가 상태에 따른 메시지를 반환합니다.
     * @return 평가 상태 메시지
     */
    public String getStatusMessage() {
        return switch (this.status) {
            case GOOD -> "README 양호";
            case CREATED -> "README 존재";
            case IMPROVEMENT -> "개선 필요";
            case NONE -> "README 없음";
        };
    }
}

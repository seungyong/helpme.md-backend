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

    /**
     * 주어진 평점에 따라 적절한 평가 상태를 설정하여 Evaluation 객체를 생성합니다. <br>
     * - 평점이 null인 경우: CREATED 상태 <br>
     * - 평점이 4.0 이상인 경우: GOOD 상태 <br>
     * - 평점이 4.0 미만인 경우: IMPROVEMENT 상태 <br>
     * - None 상태를 원한다면 {@link #createNoneStatusEvaluation} 메서드를 사용하세요.
     * @param uploaderId 평가를 업로드한 사용자 ID
     * @param repoFullName 평가 대상 저장소의 전체 이름
     * @param rating 평가 점수
     * @param content 평가 내용
     * @return 생성된 Evaluation 객체
     * @throws IllegalArgumentException 잘못된 입력 값이 제공된 경우
     */
    public static Evaluation createWithStatusEvaluation(
            Long uploaderId,
            String repoFullName,
            Float rating,
            String content
    ) {
        if (uploaderId == null) {
            throw new IllegalArgumentException("Uploader ID cannot be null.");
        } else if (rating != null && (rating < 0.0f || rating > 5.0f)) {
            if (content == null) {
                throw new IllegalArgumentException("Content cannot be null when rating is provided.");
            }

            throw new IllegalArgumentException("Rating must be between 0.0 and 5.0.");
        }

        EvaluationStatus status = getEvaluationStatus(rating);

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
     * 평가의 평점, 내용 및 상태를 변경합니다.
     * @param rating 새로운 평점
     * @param content 새로운 내용
     * @param status 새로운 평가 상태
     */
    public void changeEvaluation(Float rating, String content, EvaluationStatus status) {
        this.rating = rating;
        this.content = content;
        this.status = status;
    }

    /**
     * 평점에 따라 평가 상태를 반환합니다.
     * @param rating 평가 점수
     * @return 평가 상태
     */
    public static EvaluationStatus getEvaluationStatus(Float rating) {
        if (rating == null) { return EvaluationStatus.CREATED; }
        else if (rating >= 4.0f) { return EvaluationStatus.GOOD; }
        else { return EvaluationStatus.IMPROVEMENT; }
    }

    /**
     * content 필드를 쉼표(,)를 기준으로 분리하여 문자열 배열로 반환합니다.
     * @return content를 쉼표로 분리한 문자열 배열
     */
    public String[] getContentToArray() {
        if (this.content == null || this.content.isEmpty()) {
            return null;
        }

        return this.content.split("\n");
    }
}

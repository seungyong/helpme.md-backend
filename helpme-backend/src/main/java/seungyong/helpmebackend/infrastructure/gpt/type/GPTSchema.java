package seungyong.helpmebackend.infrastructure.gpt.type;

public final class GPTSchema {
    public static final String REPOSITORY_ANALYZE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "techStack": {
                  "type": "array",
                  "items": {
                    "type": "string",
                    "description": "리포지토리에서 사용된 기술 스택 또는 프레임워크 (예: React, Node.js, Django 등)"
                  }
                },
                "entryPoints": {
                  "type": "array",
                  "items": {
                    "type": "string",
                    "description": "프로젝트의 진입점이 되는 파일 경로, 설정이 있는 파일 경로, 의존성 파일 경로 등 (예: packange.json, build.gradle, app.js, main.py 등)"
                  }
                },
                "projectSize": {
                  "type": "string",
                  "enum": ["small", "medium", "large"],
                  "description": "프로젝트의 크기 (small, medium, large 중 하나)"
                },
                "importantFiles": {
                  "type": "array",
                  "items": {
                    "type": "string",
                    "description": "프로젝트 특성(프레임워크/언어 등)에 맞는 프로젝트의 핵심 비즈니스 로직이나 성격을 알 수 있는 중요한 파일 경로"
                  }
                }
              },
              "required": ["techStack", "entryPoints", "projectSize", "importantFiles"],
              "additionalProperties": false
            }
            """;

    public static final String EVALUATION_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "rating": {
                  "type": "number",
                  "description": "0.0에서 5.0 사이의 평가 점수"
                },
                "contents": {
                  "type": "array",
                  "items": {
                    "type": "string",
                    "description": "평가에 대한 구체적인 피드백 내용"
                  }
                }
              },
              "required": ["rating", "contents"],
              "additionalProperties": false
            }
            """;

    public static final String DRAFT_README_GENERATION_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "content": {
                  "type": "string",
                  "description": "Markdown 형식으로 생성된 README 초안의 내용"
                }
              },
              "required": ["content"],
              "additionalProperties": false
            }
            """;
}

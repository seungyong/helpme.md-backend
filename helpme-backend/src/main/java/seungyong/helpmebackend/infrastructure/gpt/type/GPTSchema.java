package seungyong.helpmebackend.infrastructure.gpt.type;

public final class GPTSchema {
    public static final String IMPORTANT_FILES_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "items": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "path": { "type": "string", "description": "파일의 경로" }
                    },
                    "required": ["path"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["items"],
              "additionalProperties": false
            }
            """;

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
                    "description": "프로젝트의 진입점이 되는 파일 또는 디렉토리 경로, 설정이 있는 파일 또는 디렉토리 경로, 의존성 파일 등 (예: src/, config/, packange.json, build.gradle, app.js, main.py 등)"
                  }
                },
                "projectSize": {
                  "type": "string",
                  "enum": ["small", "medium", "large"],
                  "description": "프로젝트의 크기 (small, medium, large 중 하나)"
                }
              },
              "required": ["techStack", "entryPoints", "projectSize"],
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
}

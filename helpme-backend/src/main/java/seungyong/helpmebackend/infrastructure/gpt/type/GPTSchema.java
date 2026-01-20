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

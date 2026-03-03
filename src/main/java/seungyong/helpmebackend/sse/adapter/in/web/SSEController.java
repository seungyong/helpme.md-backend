package seungyong.helpmebackend.sse.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.sse.application.port.in.SSEPortIn;

@Tag(
        name = "SSE",
        description = """
                SSE 관련 API
                - SSE(Server-Sent Events)는 서버에서 클라이언트로 실시간으로 데이터를 푸시하는 기술입니다.
                - `Push 평가`, `Draft 평가`, `초안 생성`에서 사용됩니다.
                """
)
@RestController
@RequestMapping("/api/v1/sse")
@ResponseBody
@RequiredArgsConstructor
public class SSEController {
    private final SSEPortIn ssePortIn;

    @GetMapping(value = "/subscribe", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter subscribe() {
        return ssePortIn.createEmitter();
    }
}

package seungyong.helpmebackend.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/oauth2")
@ResponseBody
@RequiredArgsConstructor
public class AuthController {
    @GetMapping("/github/callback")
    public void githubCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        System.out.println(
                "GitHub Callback Invoked - code: " + code + ", state: " + state
        );
    }
}

package se.findersson.synk.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    public record TestResponse(String message) {}

    @RequestMapping("")
    public TestResponse test() {
        return new TestResponse("Hello World!");
    }

}

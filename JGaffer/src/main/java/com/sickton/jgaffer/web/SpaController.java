package com.sickton.jgaffer.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all React Router client-side routes to index.html so that direct
 * URL access and browser refresh work correctly with the SPA.
 */
@Controller
public class SpaController {

    @GetMapping(value = {"/", "/clubs", "/mode", "/query", "/moments", "/moment", "/puzzle"},
                produces = "text/html")
    public String spa() {
        return "forward:/index.html";
    }
}

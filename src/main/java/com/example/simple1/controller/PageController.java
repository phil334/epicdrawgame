package com.example.simple1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {


    @SuppressWarnings("SameReturnValue")
    @GetMapping("/")
    public String redirectToEpicDraw() {
        return "redirect:/epic-draw";
    }

}

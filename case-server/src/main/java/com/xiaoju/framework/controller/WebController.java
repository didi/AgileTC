package com.xiaoju.framework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 重定向
 *
 * @author didi
 * @date 2020/9/3
 */
@Controller
public class WebController {
    @RequestMapping("/")
    public String home(){
        return "index";
    }

    @RequestMapping("/case/caseList/1")
    public String index(){
        return "index";
    }

    @RequestMapping(value ="/test/1/*")
    public String requirementId(){
        return "index";
    }

    @RequestMapping(value ="/caseManager/1/*/*/*")
    public String tcRecord(){
        return "index";
    }

    @RequestMapping(value ="/caseManager/1/*/*")
    public String tcCase(){
        return "index";
    }

}

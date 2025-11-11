package com.lyh.liuaicodemother.controller;

import com.lyh.liuaicodemother.common.BaseResponse;
import com.lyh.liuaicodemother.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class healthCheck {

    @GetMapping
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("OK");
    }

}

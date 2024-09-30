package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/common")
@Api(tags = "后台管理-通用")
public class CommonController {
    @PostMapping("/upload")
    @ApiOperation(value = "上传文件")
    public Result<String> upload(MultipartFile file) {
        // TODO 上传文件

        return Result.success();
    }
}

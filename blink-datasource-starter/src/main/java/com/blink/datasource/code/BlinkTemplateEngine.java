package com.blink.datasource.code;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.sun.istack.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 自定义模板引擎处理类 为了修改自定义生成代码的名称规则
 *
 * @author binblink
 */
public class BlinkTemplateEngine extends VelocityTemplateEngine {

    @Override
    protected void outputCustomFile(@NotNull List<CustomFile> customFiles, @NotNull TableInfo tableInfo, @NotNull Map<String, Object> objectMap) {
        String parentPath = getPathInfo(OutputFile.parent);
        customFiles.forEach(file -> {
            String filePath = StringUtils.isNotBlank(file.getFilePath()) ? file.getFilePath() : parentPath;
            if (StringUtils.isNotBlank(file.getPackageName())) {
                filePath = filePath + File.separator + file.getPackageName();
                filePath = filePath.replaceAll("\\.", StringPool.BACK_SLASH + File.separator);
            }
            String fileName = filePath + File.separator  + getFileName(tableInfo,file);
            outputFile(new File(fileName), objectMap, file.getTemplatePath(), file.isFileOverride());
        });
    }

    private String getFileName(TableInfo tableInfo,CustomFile file){
        String entityName = tableInfo.getEntityName();
        entityName = entityName.replaceAll("DO","");

        String fileName = ".java";
        String tempPath = file.getTemplatePath().replaceAll("\\.java.vm", "");
        if("dto.req".equals(file.getPackageName())){
            String tempName =  tempPath.substring(tempPath.lastIndexOf("/")+1);
            String prefix = tempName.substring(0,tempName.indexOf("ReqDTO"));
            String endfix = tempName.substring(tempName.indexOf("ReqDTO"));
            fileName = prefix + entityName + endfix + fileName;
            return fileName;

        }

        if("dto.rsp".equals(file.getPackageName())){
            String tempName =  tempPath.substring(tempPath.lastIndexOf("/")+1);
            String prefix = tempName.substring(0,tempName.indexOf("RspDTO"));
            String endfix = tempName.substring(tempName.indexOf("RspDTO"));
            fileName = prefix + entityName + endfix + fileName;
            return fileName;
        }

        if("test".equals(file.getPackageName())){
            fileName = entityName + "ControllerTest"  + fileName;
            return fileName;
        }
        return fileName;
    }

}




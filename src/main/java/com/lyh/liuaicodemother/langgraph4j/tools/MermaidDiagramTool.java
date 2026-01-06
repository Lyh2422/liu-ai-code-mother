package com.lyh.liuaicodemother.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.lyh.liuaicodemother.exception.BusinessException;
import com.lyh.liuaicodemother.exception.ErrorCode;
import com.lyh.liuaicodemother.langgraph4j.model.ImageCategoryEnum;
import com.lyh.liuaicodemother.langgraph4j.model.ImageResource;
import com.lyh.liuaicodemother.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Mermaid 架构图生成工具(将 Mermaid 代码转换为架构图图片)
 */
@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;

    /**
     * mmdc的绝对路径（终端执行 which mmdc 得到的路径）
     */
    private static final String MMDC_ABSOLUTE_PATH = "/Users/Zhuanz1/.npm-global/bin/mmdc";

    /**
     * Chrome的绝对路径（Mac下Google Chrome的默认路径）
     */
    private static final String CHROME_ABSOLUTE_PATH = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // 上传到COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 清理临时文件
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);

        // 构建命令（移除--chrome-path参数）
        String mmdcCommand = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : MMDC_ABSOLUTE_PATH;
        String cmdLine = String.format("%s -i %s -o %s -b transparent",
                mmdcCommand,
                tempInputFile.getAbsolutePath(),
                tempOutputFile.getAbsolutePath()
        );

        log.info("执行Mermaid转换命令：{}", cmdLine);

        // ========== 核心修改：用ProcessBuilder手动执行命令（兼容hutool 5.8.38） ==========
        Process process = null;
        StringBuilder execResult = new StringBuilder();
        try {
            // 1. 拆分命令（ProcessBuilder推荐传入字符串数组，避免空格问题）
            String[] commandArray = cmdLine.split(" ");
            // 2. 创建ProcessBuilder并设置命令
            ProcessBuilder pb = new ProcessBuilder(commandArray);
            // 3. 设置环境变量（添加Chrome路径）
            Map<String, String> env = pb.environment();
            env.put("PUPPETEER_EXECUTABLE_PATH", CHROME_ABSOLUTE_PATH);
            // 4. 重定向错误流到标准输出（方便捕获所有输出）
            pb.redirectErrorStream(true);
            // 5. 启动进程
            process = pb.start();

            // 6. 读取命令执行结果
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    execResult.append(line).append("\n");
                }
            }

            // 7. 等待命令执行完成，获取退出码
            int exitCode = process.waitFor();
            log.info("Mermaid命令执行退出码：{}，执行结果：{}", exitCode,
                    StrUtil.isBlank(execResult.toString()) ? "执行成功，无输出" : execResult);

            // 8. 检查退出码（0表示执行成功）
            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "mmdc命令执行失败，退出码：" + exitCode + "，错误信息：" + execResult);
            }

        } catch (Exception e) {
            log.error("执行mmdc命令失败，命令：{}", cmdLine, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "执行mmdc命令失败：" + e.getMessage());
        } finally {
            // 确保进程关闭
            if (process != null) {
                process.destroy();
            }
        }

        // 检查输出文件
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 执行失败，未生成SVG文件");
        }
        // 清理输入文件，保留输出文件供上传使用
        FileUtil.del(tempInputFile);
        return tempOutputFile;
    }
}
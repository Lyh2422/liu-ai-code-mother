package com.lyh.liuaicodemother.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.lyh.liuaicodemother.exception.BusinessException;
import com.lyh.liuaicodemother.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Slf4j
public class WebScreenshotUtils {

    /**
     * 初始化 Firefox 浏览器驱动（修正命名，移除Chrome相关配置）
     */
    private static WebDriver initFirefoxDriver(int width, int height) {
        try {
            // 1. 自动管理Firefox的驱动（GeckoDriver）
            WebDriverManager.firefoxdriver().setup();

            // 2. 配置Firefox选项（仅保留Firefox支持的参数，移除Chrome专属参数）
            FirefoxOptions options = new FirefoxOptions();

            // 关键：指定Firefox二进制文件路径（Mac系统，适配官网安装和Homebrew安装的路径）
            // 路径1：官网安装的Firefox路径
            File firefoxBinary = new File("/Applications/Firefox.app/Contents/MacOS/firefox");
            // 路径2：Homebrew安装的Firefox路径（如果上面的路径不存在，用这个）
            if (!firefoxBinary.exists()) {
                firefoxBinary = new File("/Applications/Firefox Developer Edition.app/Contents/MacOS/firefox");
            }
            // 设置Firefox二进制路径（解决“找不到binary”的错误）
            options.setBinary(firefoxBinary.toPath());

            // Firefox的无头模式（新版支持--headless=new，旧版用--headless，这里兼容两者）
            options.addArguments("--headless=new");
            // 设置窗口大小（Firefox支持该参数）
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展（Firefox支持该参数）
            options.addArguments("--disable-extensions");
            // 设置用户代理（Firefox支持该参数）
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // 可选：禁用Firefox的自动更新和弹窗（提升稳定性）
            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("app.update.enabled", false);
            profile.setPreference("browser.tabs.remote.autostart", false);
            options.setProfile(profile);

            // 3. 创建Firefox驱动
            WebDriver driver = new FirefoxDriver(options);

            // 4. 超时配置
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            return driver;
        } catch (Exception e) {
            log.error("初始化 Firefox 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Firefox 浏览器失败");
        }
    }

    /**
     * 生成网页截图（仅修改驱动调用方法，其余业务逻辑不变）
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return null;
        }

        // 自动补全localhost前缀（原有逻辑不变）
        String validUrl;
        try {
            new URL(webUrl);
            validUrl = webUrl;
        } catch (MalformedURLException e) {
            validUrl = "http://localhost/" + webUrl;
            log.info("传入的是短标识，自动补全为完整URL: {}", validUrl);
        }

        WebDriver webDriver = null;
        try {
            final int DEFAULT_WIDTH = 1600;
            final int DEFAULT_HEIGHT = 900;
            // 关键修改：调用Firefox驱动初始化方法
            webDriver = initFirefoxDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);

            // 创建临时目录（原有逻辑不变）
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 原始截图路径
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + ".png";
            // 访问补全后的URL
            webDriver.get(validUrl);
            // 等待页面加载
            waitForPageLoad(webDriver);
            // 截图并保存
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);
            // 压缩图片
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + "_compressed.jpg";
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功: {}", compressedImagePath);
            // 删除原始图片
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            return null;
        } finally {
            // 关闭驱动（修正日志提示为Firefox）
            if (webDriver != null) {
                try {
                    webDriver.quit();
                    log.info("FirefoxDriver已成功关闭");
                } catch (Exception e) {
                    log.error("关闭FirefoxDriver失败", e);
                }
            }
        }
    }

    /**
     * 保存图片到文件（原有逻辑不变）
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片（原有逻辑不变）
     */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 等待页面加载完成（原有逻辑不变）
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }
}
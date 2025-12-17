package com.lyh.liuaicodemother.service;


import com.lyh.liuaicodemother.model.dto.app.AppAddRequest;
import com.lyh.liuaicodemother.model.dto.app.AppQueryRequest;
import com.lyh.liuaicodemother.model.entity.App;
import com.lyh.liuaicodemother.model.entity.User;
import com.lyh.liuaicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author lyh
 */
public interface AppService extends IService<App> {

    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 获取应用封装类
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取查询包装类
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 生成代码
     * @param appId 应用Id
     * @param message 生成代码请求
     * @param loginUser 登录用户
     * @return 生成代码结果
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 应用部署
     * @param appId 应用Id
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     * @param appId 应用Id
     * @param appUrl 应用访问Url
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);
}

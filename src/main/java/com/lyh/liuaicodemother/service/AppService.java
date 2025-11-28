package com.lyh.liuaicodemother.service;


import com.lyh.liuaicodemother.model.dto.app.AppQueryRequest;
import com.lyh.liuaicodemother.model.entity.App;
import com.lyh.liuaicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author lyh
 */
public interface AppService extends IService<App> {

    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);

}

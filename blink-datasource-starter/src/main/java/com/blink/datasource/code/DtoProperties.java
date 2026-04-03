package com.blink.datasource.code;

import java.io.Serializable;

/**
 * 模版自定义属性
 */
public class DtoProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * DTO请求实体包路径
     */
    private String reqDtoPackage;

    /**
     * DTO响应实体包路径
     */
    private String rspDtoPackage;

    /**
     * DTO新增实体请求类名
     */
    private String addReqDtoName;

    /**
     * DTO删除实体请求类名
     */
    private String deleteReqDtoName;

    /**
     * DTO更新实体请求类名
     */
    private String updateReqDtoName;

    /**
     * DTO查询列表实体请求类名
     */
    private String queryReqDtoName;

    /**
     * DTO查询列表实体响应类名
     */
    private String queryRspDtoName;

    public String getReqDtoPackage() {
        return reqDtoPackage;
    }

    public void setReqDtoPackage(String reqDtoPackage) {
        this.reqDtoPackage = reqDtoPackage;
    }

    public String getRspDtoPackage() {
        return rspDtoPackage;
    }

    public void setRspDtoPackage(String rspDtoPackage) {
        this.rspDtoPackage = rspDtoPackage;
    }

    public String getAddReqDtoName() {
        return addReqDtoName;
    }

    public void setAddReqDtoName(String addReqDtoName) {
        this.addReqDtoName = addReqDtoName;
    }

    public String getDeleteReqDtoName() {
        return deleteReqDtoName;
    }

    public void setDeleteReqDtoName(String deleteReqDtoName) {
        this.deleteReqDtoName = deleteReqDtoName;
    }

    public String getUpdateReqDtoName() {
        return updateReqDtoName;
    }

    public void setUpdateReqDtoName(String updateReqDtoName) {
        this.updateReqDtoName = updateReqDtoName;
    }

    public String getQueryReqDtoName() {
        return queryReqDtoName;
    }

    public void setQueryReqDtoName(String queryReqDtoName) {
        this.queryReqDtoName = queryReqDtoName;
    }

    public String getQueryRspDtoName() {
        return queryRspDtoName;
    }

    public void setQueryRspDtoName(String queryRspDtoName) {
        this.queryRspDtoName = queryRspDtoName;
    }
}

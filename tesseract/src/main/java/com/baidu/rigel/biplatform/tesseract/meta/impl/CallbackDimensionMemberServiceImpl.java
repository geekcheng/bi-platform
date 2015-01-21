/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.rigel.biplatform.tesseract.meta.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.baidu.rigel.biplatform.ac.exception.MiniCubeQueryException;
import com.baidu.rigel.biplatform.ac.minicube.CallbackLevel;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeMember;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Level;
import com.baidu.rigel.biplatform.ac.model.Member;
import com.baidu.rigel.biplatform.ac.model.callback.CallbackDimTreeNode;
import com.baidu.rigel.biplatform.ac.model.callback.CallbackResponse;
import com.baidu.rigel.biplatform.ac.model.callback.CallbackServiceInvoker;
import com.baidu.rigel.biplatform.ac.model.callback.CallbackType;
import com.baidu.rigel.biplatform.ac.model.callback.ResponseStatus;
import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.tesseract.exception.MetaException;
import com.baidu.rigel.biplatform.tesseract.meta.DimensionMemberService;
import com.google.common.collect.Sets;

/**
 * callback获取member的实现
 * 
 * @author xiaoming.chen
 *
 */
@Service(DimensionMemberService.CALLBACK_MEMBER_SERCICE)
public class CallbackDimensionMemberServiceImpl implements DimensionMemberService {

    /**
     * log
     */
    private Logger log = Logger.getLogger(this.getClass());

    /**
     * treeCallbackService TODO 这个后续修改，会变成从工厂获取
     */
//    private static CallbackServiceImpl treeCallbackService = new CallbackServiceImpl();

    @Override
    public List<MiniCubeMember> getMembers(Cube cube, Level level, DataSourceInfo dataSourceInfo, Member parentMember,
            Map<String, String> params) throws MiniCubeQueryException, MetaException {
//        MetaDataService.checkCube(cube);
//        MetaDataService.checkDataSourceInfo(dataSourceInfo);
        CallbackLevel callbackLevel = (CallbackLevel) level;
        
        CallbackResponse response = 
                CallbackServiceInvoker.invokeCallback(callbackLevel.getCallbackUrl(), params, CallbackType.DIM);
        if (response.getStatus() == ResponseStatus.SUCCESS) {
            @SuppressWarnings("unchecked")
            List<CallbackDimTreeNode> posTree = (List<CallbackDimTreeNode>) response.getData();
            List<MiniCubeMember> result = createMembersByPosTreeNode(posTree, level, null);
            if (parentMember == null) {
                return result;
            } else {
                return createMembersByPosTreeNode(posTree.get(0).getChildren(), level, null);
            }
        } else {
            log.error("[ERROR] --- --- " + response.getStatus() + "---" + response.getMessage());
            // 错误请求，直接提示出错
            throw new RuntimeException(response.getMessage());
        }
        
    }

    /**
     * 
     * @param posTree
     * @param level
     * @param parent
     * @return
     */
    private List<MiniCubeMember> createMembersByPosTreeNode(List<CallbackDimTreeNode> posTree, Level level,
            MiniCubeMember parent) {
        List<MiniCubeMember> members = new ArrayList<MiniCubeMember>();
        if (CollectionUtils.isNotEmpty(posTree)) {
            for (CallbackDimTreeNode node : posTree) {
                MiniCubeMember member = createMemberByPosTreeNode(node, level, parent);
                member.setChildren(createMembersByPosTreeNode(node.getChildren(), level, member));
                members.add(member);
            }
        }
        return members;
    }

//    /**
//     * 根据Callback的level获取Callback
//     * 
//     * @param level CallbackLevel
//     * @param params 参数信息
//     * @return Callback返回结果
//     * @throws IOException Http请求异常
//     */
//    private List<CallBackTreeNode> fetchCallBack(Level level, Map<String, String> params) throws MiniCubeQueryException {
//        if (level == null || !level.getType().equals(LevelType.CALL_BACK)) {
//            throw new IllegalArgumentException("level type must be call back:" + level);
//        }
//        CallbackLevel callbackLevel = (CallbackLevel) level;
//        if (StringUtils.isBlank(callbackLevel.getCallbackUrl())) {
//            throw new IllegalArgumentException("callback url can not be empty:" + callbackLevel);
//        }
//        Map<String, String> callbackParams = Maps.newHashMap(callbackLevel.getCallbackParams());
//        if (MapUtils.isNotEmpty(params)) {
//            callbackParams.putAll(params);
//        }
//
//        // 默认设置只取本身和本身的孩子节点
//        List<CallBackTreeNode> result;
//        try {
//            result = treeCallbackService.fetchCallback(callbackLevel.getCallbackUrl(), callbackParams);
//            return result;
//        } catch (IOException e) {
//            log.error("fetch callback error,url:" + callbackLevel.getCallbackUrl() + " params:" + callbackParams, e);
//            throw new MiniCubeQueryException(e);
//        }
//    }

    @Override
    public MiniCubeMember getMemberFromLevelByName(DataSourceInfo dataSourceInfo, Cube cube, Level level, String name,
            MiniCubeMember parent, Map<String, String> params) throws MiniCubeQueryException, MetaException {
//        MetaDataService.checkCube(cube);
//        MetaDataService.checkDataSourceInfo(dataSourceInfo);
//        List<CallBackTreeNode> posTree = fetchCallBack(level, params);
        CallbackLevel callbackLevel = (CallbackLevel) level;
        CallbackResponse response = 
                CallbackServiceInvoker.invokeCallback(callbackLevel.getCallbackUrl(), params, CallbackType.DIM);
        if (response.getStatus() == ResponseStatus.SUCCESS) {
            @SuppressWarnings("unchecked")
            List<CallbackDimTreeNode> posTree = (List<CallbackDimTreeNode>) response.getData();
            if (posTree.size() != 1) {
                throw new MiniCubeQueryException("pos tree return over 1 node:" + posTree);
            }
            
            return createMemberByPosTreeNode(posTree.get(0), level, null);
            
        } else {
            throw new RuntimeException(response.getMessage());
        }
    }

    /**
     * @param node
     * @param level
     * @return
     */
    private MiniCubeMember createMemberByPosTreeNode(CallbackDimTreeNode node, Level level, Member parentMember) {
        MiniCubeMember result = new MiniCubeMember(node.getId());
        result.setLevel(level);
        result.setCaption(node.getName());
        if (CollectionUtils.isNotEmpty(node.getCsIds())) {
            result.setQueryNodes(Sets.newHashSet(node.getCsIds()));
        }
        // 先生成一下uniqueName，避免后续生成带上了父节点的UniqueName
        result.generateUniqueName(null);
        result.setParent(parentMember);
        return result;
    }

}

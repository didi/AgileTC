package com.xiaoju.framework.service.impl;

import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.dto.DirNodeDto;
import com.xiaoju.framework.entity.exception.CaseServerException;
import lombok.Data;
import org.apache.commons.compress.utils.Sets;

import java.util.HashSet;

/**
 * Created by didi on 2021/12/14.
 */
@Data
public class DirMoveDFS {
    private String fromId;

    private String toId;

    private DirNodeDto fromObj;

    private DirNodeDto toObj;

    private DirNodeDto parent;

    /**
     * 用于记录找到set的id集合，用来判断是不是从父文件夹移到了子文件夹，这样的操作不允许
     */
    private HashSet<String> toSet;

    public DirMoveDFS(String fromId, String toId) {
        this.fromId = fromId;
        this.toId = toId;
        this.fromObj = null;
        this.toObj = null;
        this.parent = null;
        this.toSet = new HashSet<>();
    }

    public void findNodeAndDelete(DirNodeDto node) {
        findNode(node, Sets.newHashSet(node.getId()));
        if (parent == null) {
            throw new CaseServerException("要去的文件夹不正确", StatusCode.INTERNAL_ERROR);
        }
        if (toSet.contains(fromObj.getId())) {
            throw new CaseServerException("不能从父文件夹移到子文件夹", StatusCode.INTERNAL_ERROR);
        }

        parent.getChildren().remove(fromObj);
    }

    private void findNode(DirNodeDto node, HashSet<String> set) {
        if (node == null) {
            return ;
        }
        if(toId.equals(node.getId())){
            toObj = node;
        }
        for (DirNodeDto child : node.getChildren()) {
            set.add(node.getId());
            if (fromId.equals(child.getId())) {
                // 找到了需要移动的文件夹，并且记录其父节点和当前节点
                fromObj = child;
                parent = node;
            }
            if (toId.equals(child.getId())) {
                // 找到了要移过去的文件夹，就记录是哪个节点，并且留存经过的id路径
                toObj = child;
                toSet = new HashSet<>(set);
            }
            findNode(child, set);
            set.remove(node.getId());
        }
    }
}

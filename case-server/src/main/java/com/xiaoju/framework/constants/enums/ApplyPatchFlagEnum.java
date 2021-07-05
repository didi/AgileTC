package com.xiaoju.framework.constants.enums;

public enum ApplyPatchFlagEnum {

    /**
     * 忽略对 order replace 操作时出现的冲突
     * 在测试任务编辑时，因为展示的脑图是完整脑图的子集，各个字段的 order 值和完整版会不一样
     * 所以一旦改动节点顺序，replace 原始的 order 值基本都会是错的，引起冲突
     */
    IGNORE_REPLACE_ORDER_CONFLICT,

    /**
     * 忽略对节点展开属性变更导致的冲突。这个冲突不影响实际用例数据，只影响展示效果
     */
    IGNORE_EXPAND_STATE_CONFLICT
}

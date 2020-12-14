package com.xiaoju.framework.entity.xmind;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 用例计算成功、失败、阻塞、总计的结构体
 *
 * @author didi
 * @date 2020/8/17
 */
@Data
public class CaseCount {

    /**
     * 成功用例数
     */
    private int success;

    /**
     * 失败用例数
     */
    private int fail;

    /**
     * 阻塞用例数
     */
    private int block;

    /**
     * 不执行用例数
     */
    private int ignore;

    /**
     * 用例总数
     */
    private int total;

    /**
     * 遍历时把操作记录拉进来
     */
    private JSONObject progress = new JSONObject();

    public void addProgress(String id, Object progressObj) {
        if (progressObj != null) {
            progress.put(id, progressObj);
        }
    }

    public void addAllProgress(JSONObject obj) {
        if (obj != null) {
            progress.putAll(obj);
        }
    }

    public JSONObject getProgress() {
        return progress;
    }

    /**
     * 获取用例执行数
     * 即失败的+成功的+阻塞的个数总和
     * 表示用户操作过了
     */
    public int getPassCount() {
        return success + fail + block;
    }

    /**
     * 这里与直接带参的方法不同
     * 无参方法主要针对最深的叶节点，也就是没有子节点的节点，进行简单的++
     * 而有参方法主要针对根&树干节点，也就是有子节点的节点，需要把最深的叶节点的信息带过来
     */
    public void addSuccess() {
        success ++;
        addTotal();
    }

    public void combineSuccess(int num) {
        clear();
        addTotal(num);
        success = total;
    }

    public void addFail() {
        fail ++;
        addTotal();
    }

    public void combineFail(int num) {
        clear();
        addTotal(num);
        fail = total;
    }

    public void addBlock() {
        block ++;
        addTotal();
    }

    public void combineBlock(int num) {
        clear();
        addTotal(num);
        block = total;
    }

    public void addIgnore() {
        ignore ++;
        // 发现节点为不执行后，当前节点和后续节点total均为0
        total = 0;
    }

    public void combineIgnore(int num) {
        clear();
        ignore = num;
        total = 0;
    }

    public void addTotal() {
        total ++;
    }

    public void addTotal(int num) {
        total += num;
    }

    private void clear() {
        this.success = 0;
        this.block = 0;
        this.fail = 0;
    }

    /**
     * 将别的计数体数据 加到自己身上
     *
     * @param count 另外节点的计数体
     */
    public void cover(CaseCount count) {
        this.success += count.getSuccess();
        this.fail += count.getFail();
        this.block += count.getBlock();
        this.ignore += count.getIgnore();
        this.total += count.getTotal();
        this.progress.putAll(count.progress);
    }


}

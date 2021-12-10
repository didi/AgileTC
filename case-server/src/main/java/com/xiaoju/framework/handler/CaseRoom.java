package com.xiaoju.framework.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.entity.persistent.CaseBackup;
import com.xiaoju.framework.entity.persistent.TestCase;
import com.xiaoju.framework.util.BitBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by didi on 2021/3/27.
 */
public class CaseRoom extends Room {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseRoom.class);

    public CaseRoom(Long id) {
        super(id);
    }

    @Override
    public Player createAndAddPlayer(Client client) {
        Player p = super.createAndAddPlayer(client);
        String caseContent = testCaseContent != null ? testCaseContent : testCase.getCaseContent();
        p.getClient().sendMessage(CaseMessageType.EDITOR, caseContent);
        LOGGER.info(Thread.currentThread().getName() + ": 新的用户加入成功");
        return p;
    }

    @Override
    protected void internalRemovePlayer(Player p) {
        super.internalRemovePlayer(p);

        // 如果是最后一个用户离开，需要关闭广播任务
        if (players.size() == 0) {
            if (testCaseContent != null) {

                CaseBackup caseBackup = new CaseBackup();
                caseBackup.setCaseContent(testCaseContent);
                caseBackup.setCreator(p.getClient().getClientName());
                caseBackup.setTitle("离开自动保存用例");
                caseBackup.setCaseId(testCase.getId());
                caseBackupService.insertBackup(caseBackup);
            }
            synchronized (WebSocket.getRoomLock()) {
                if (testCaseContent != null) {
                    TestCase testCaseBase = caseMapper.selectOne(testCase.getId());


                    JSONObject caseContentToSave = JSON.parseObject(testCaseContent);
                    JSONObject caseContentBase = JSON.parseObject(testCaseBase.getCaseContent());
                    if (caseContentToSave.getInteger("base") > caseContentBase.getInteger("base")) {
                        // 保存落库
                        testCase.setCaseContent(testCaseContent);
                        testCase.setGmtModified(new Date(System.currentTimeMillis()));
                        int ret = caseMapper.update(testCase);
                        if (ret < 1) {
                            LOGGER.error(Thread.currentThread().getName() + ": 数据库用例内容更新失败。 ret = " + ret);
                            LOGGER.error("应该保存的用例内容是：" + testCaseContent);
                        }
                    } else {
                        // 不保存
                        LOGGER.info(Thread.currentThread().getName() + "不落库." + testCaseContent );
                    }

                }
                WebSocket.getRooms().remove(Long.valueOf(BitBaseUtil.mergeLong(0l, Long.valueOf(testCase.getId()))));
                LOGGER.info(Thread.currentThread().getName() + ": [websocket-onClose 关闭当前Room成功]当前sessionid:" + p.getClient().getSession().getId());
            }

            LOGGER.info(Thread.currentThread().getName() + ": 最后一名用户离开，关闭。");
        }

        // 广播有用户离开
        Set<String> names = new HashSet<>();
        for (Client c : cs.values()) {
            names.add(c.getClientName());
        }
        broadcastRoomMessage(CaseMessageType.NOTIFY, "当前用户数:" + players.size() + ",用户是:" + String.join(",", names));

//        broadcastRoomMessage(CaseMessageType.NOTIFY, "当前用户数:" + players.size() + "。用例编辑者 " + p.getClient().getClientName() + " 离开");
        LOGGER.info(Thread.currentThread().getName() + ": 用例保存完成。");

    }
}

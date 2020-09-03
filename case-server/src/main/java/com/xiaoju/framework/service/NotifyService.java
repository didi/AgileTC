package com.xiaoju.framework.service;

import com.xiaoju.framework.entity.ExecRecord;

import java.util.List;

public interface NotifyService {

     void sendCreateNotify(ExecRecord execRecord, String link, String sendDchatApi, String sendEmailApi, String token, String sender);

     void sendEditNotify(ExecRecord oldRecord, ExecRecord execRecord, String link, String sendDchatApi, String sendEmailApi, String token, String sender);
}

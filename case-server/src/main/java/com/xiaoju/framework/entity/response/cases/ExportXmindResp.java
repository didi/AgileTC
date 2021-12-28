package com.xiaoju.framework.entity.response.cases;

import lombok.Data;

/**
 * 导出的xmind所含有的内容
 *
 * @author hcy
 * @date 2020/10/27
 */
@Data
public class ExportXmindResp {

    private String fileName;

    private byte[] data;
}

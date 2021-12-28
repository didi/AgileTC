package com.xiaoju.framework.entity.response.dir;

import com.xiaoju.framework.entity.dto.DirNodeDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 树结构
 *
 * @author hcy
 * @date 2020/11/11
 */
@Data
public class DirTreeResp {

    private List<DirNodeDto> children = new ArrayList<>();
}

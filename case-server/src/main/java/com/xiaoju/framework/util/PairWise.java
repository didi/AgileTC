package com.xiaoju.framework.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PairWise  {
    public static List<String> solution(String content) {
        List<String> res = new ArrayList<>();
        if (content == null) {
            return res;
        }
        JSONObject contJson = JSON.parseObject(content);
        String[][] str = new String[contJson.size()][];
        String[] keys = new String[contJson.size()];
        int m = 0;
        int sum = 1;
        for (Map.Entry entry: contJson.entrySet()) {
            String elementValues[] = entry.getValue().toString().trim().split(",");
            keys[m] = entry.getKey().toString();
            str[m] = new String[elementValues.length];
            for (int j = 0; j < elementValues.length; j ++) {
                str[m][j] = elementValues[j].trim();
            }
            m ++;
            sum *= elementValues.length;
        }

        // 符合要求的测试用例数量
        int count = 0;
        Map<String, Integer> hashMap = new HashMap<>();

        int[] one = new int[str.length];
        for (int i = 0; i < sum; i++) {
            // 创造一个新的测试用例
            int carry = 1;
            for (int j = str.length - 1; j >= 0; j--) {
                if (i == 0) {
                    continue;
                }
                one[j] = (one[j] + carry) % str[j].length;
                if (carry == 1 && one[j] == 0) {
                    carry = 1;
                } else {
                    carry = 0;
                }
            }
            // 测试该测试用例是否能够产生新的配对组
            boolean flag = false;
            for (int j = 0; j < str.length; j++) {
                for (int k = j + 1; k < str.length; k++) {
                    String key = j + "_" + str[j][one[j]] + "," + k + "_" + str[k][one[k]];
                    if (hashMap.get(key) == null) {
                        flag = true;
                        hashMap.put(key, 1);
                    }
                }
            }

            // 产生了新的配对组，说明该用例符合 PairWise 规则，输出
            if (flag) {
                String caseContent = "";
                count++;
                caseContent = keys[0] + "取值是" + str[0][one[0]];
                for (int j = 1; j < str.length; j++) {
                    caseContent = caseContent + "," + keys[j] + "取值是" +  str[j][one[j]];
                    System.out.print("," + str[j][one[j]]);
                }
                System.out.println();
                res.add(caseContent);
            }
        }
        return res;
    }

    public static void main(String[] args) {
//        solution(new String[][]{{"T", "F"}, {"1", "2", "3"}, {"a", "b", "c", "d"}});
//        String aa = "11|ss|xxxx|xx";
//        int a1 = aa.indexOf('|');
//        System.out.println(a1);
//        System.out.println(aa.substring(a1));
//        System.out.println(aa.substring(0, a1));
        List<String> ret = solution("{\"1\":\"1,2\",\"2\":\"2,3\"}");
        System.out.println(ret.toString());
    }
}

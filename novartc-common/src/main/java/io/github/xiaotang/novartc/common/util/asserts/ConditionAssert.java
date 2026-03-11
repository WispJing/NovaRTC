package io.github.xiaotang.novartc.common.util.asserts;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;

/**
 * Created on 2026/3/11 11:50
 *
 * @author tangj
 * @description
 */
public class ConditionAssert {
    private final boolean condition;

    ConditionAssert(boolean condition) {
        this.condition = condition;
    }

    public void isTrue(ResultCode code) {
        if (!condition) {
            throw new BusinessException(code);
        }
    }

    public void isTrue(String message) {
        if (!condition) {
            throw new BusinessException(-1, message);
        }
    }
}

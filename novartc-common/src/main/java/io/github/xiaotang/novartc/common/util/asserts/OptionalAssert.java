package io.github.xiaotang.novartc.common.util.asserts;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;

/**
 * Created on 2026/3/11 11:50
 *
 * @author tangj
 * @description
 */
public class OptionalAssert<T> {
    private final T value;

    OptionalAssert(T value) {
        this.value = value;
    }

    public T orElseThrow(ResultCode code) {
        if (value == null) {
            throw new BusinessException(code);
        }
        return value;
    }

    public T orElseThrow(String message) {
        if (value == null) {
            throw new BusinessException(-1, message);
        }
        return value;
    }
}

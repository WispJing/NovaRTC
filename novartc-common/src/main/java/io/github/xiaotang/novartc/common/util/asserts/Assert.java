package io.github.xiaotang.novartc.common.util.asserts;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.Result;
import io.github.xiaotang.novartc.common.result.ResultCode;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created on 2026/3/11 11:50
 *
 * @author tangj
 * @description
 */
public class Assert<T> {
    private final T value;

    private Assert(T value) {
        this.value = value;
    }

    /**
     * 对象断言
     */
    public static <T> Assert<T> of(T value) {
        return new Assert<>(value);
    }

    /**
     * Optional 风格
     */
    public static <T> OptionalAssert<T> ofNullable(T value) {
        return new OptionalAssert<>(value);
    }

    /**
     * 条件断言
     */
    public static ConditionAssert that(boolean condition) {
        return new ConditionAssert(condition);
    }

    /**
     * 不为 null
     */
    public Assert<T> notNull(ResultCode code) {
        if (value == null) {
            throw new BusinessException(code);
        }
        return this;
    }

    /**
     * 自动识别 empty
     */
    public Assert<T> notEmpty(ResultCode code) {
        return switch (value) {
            case null -> throw new BusinessException(code);
            case String str when str.trim().isEmpty() -> throw new BusinessException(code);
            case Collection<?> col when col.isEmpty() -> throw new BusinessException(code);
            case Map<?, ?> map when map.isEmpty() -> throw new BusinessException(code);
            default -> this;
        };
    }

    /**
     * Lambda断言
     */
    public Assert<T> check(Predicate<T> predicate, ResultCode code) {
        if (!predicate.test(value)) {
            throw new BusinessException(code);
        }
        return this;
    }

    public Assert<T> check(Predicate<T> predicate, String message) {
        if (!predicate.test(value)) {
            throw new BusinessException(-1, message);
        }
        return this;
    }
}

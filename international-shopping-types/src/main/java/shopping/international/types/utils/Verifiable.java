package shopping.international.types.utils;

/**
 * <p>定义了可验证对象的基本行为, 任何实现了此接口的类都必须能够通过验证自身状态或属性来确保其符合预设条件</p>
 *
 * <p>该接口提供了一组默认方法, 用于在创建和更新操作前执行验证, 如果验证失败则抛出异常。这些方法依赖于 {@link #validate()} 方法来完成具体的验证逻辑</p>
 */
public interface Verifiable {
    /**
     * 验证当前对象是否符合预定义的规则或条件
     *
     * <p>此方法用于确保对象的状态或属性满足特定的要求, 如果验证失败, 则可能抛出异常来指示问题所在</p>
     *
     * @throws IllegalArgumentException 如果对象不符合要求, 该异常包含了具体的错误信息
     */
    void validate();

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    default void createValidate() {
        validate();
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象在更新操作前是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    default void updateValidate() {
        validate();
    }
}

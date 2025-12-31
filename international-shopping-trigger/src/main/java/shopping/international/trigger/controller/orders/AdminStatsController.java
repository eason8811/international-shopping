package shopping.international.trigger.controller.orders;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.orders.OrderStatsOverviewRespond;
import shopping.international.api.resp.orders.OrderStatsRowRespond;
import shopping.international.domain.model.enums.orders.OrderStatsDimension;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.vo.orders.OrderStatsOverviewQuery;
import shopping.international.domain.model.vo.orders.OrderStatsQuery;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.orders.IAdminStatsService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 管理侧订单统计接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin/orders/stats")
public class AdminStatsController {

    /**
     * 管理侧统计领域服务
     */
    private final IAdminStatsService adminStatsService;
    /**
     * 货币配置服务
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 订单统计概览 (时间范围)
     *
     * @param from     时间起
     * @param to       时间止
     * @param status   状态过滤 (可为空)
     * @param currency 币种过滤 (可为空)
     * @return 概览
     */
    @GetMapping("/overview")
    public ResponseEntity<Result<OrderStatsOverviewRespond>> overview(@RequestParam("from") String from,
                                                                      @RequestParam("to") String to,
                                                                      @RequestParam(required = false) String status,
                                                                      @RequestParam(required = false) String currency) {
        LocalDateTime fromTime = requireDateTime(from, "from");
        LocalDateTime toTime = requireDateTime(to, "to");
        OrderStatus statusEnum = parseEnum(status, OrderStatus.class);
        OrderStatsOverviewQuery query = OrderStatsOverviewQuery.builder()
                .from(fromTime)
                .to(toTime)
                .status(statusEnum)
                .currency(currency)
                .build();
        query.validate();
        IAdminStatsService.OrderStatsOverviewView overview = adminStatsService.overview(query);
        CurrencyConfig currencyConfig = query.getCurrency() == null ? null : currencyConfigService.get(query.getCurrency());

        OrderStatsOverviewRespond resp = OrderStatsOverviewRespond.builder()
                .from(fromTime)
                .to(toTime)
                .currency(query.getCurrency())
                .ordersCount(overview.ordersCount() == null ? 0 : overview.ordersCount().intValue())
                .paidOrdersCount(overview.paidOrdersCount() == null ? 0 : overview.paidOrdersCount().intValue())
                .itemsCountSum(overview.itemsCount() == null ? null : overview.itemsCount().intValue())
                .totalAmountSum(currencyConfig == null || overview.totalAmountMinor() == null ? null : currencyConfig.toMajor(overview.totalAmountMinor()).toPlainString())
                .discountAmountSum(currencyConfig == null || overview.discountAmountMinor() == null ? null : currencyConfig.toMajor(overview.discountAmountMinor()).toPlainString())
                .shippingAmountSum(currencyConfig == null || overview.shippingAmountMinor() == null ? null : currencyConfig.toMajor(overview.shippingAmountMinor()).toPlainString())
                .payAmountSum(currencyConfig == null || overview.payAmountMinor() == null ? null : currencyConfig.toMajor(overview.payAmountMinor()).toPlainString())
                .build();

        return ResponseEntity.ok(Result.ok(resp));
    }

    /**
     * 订单统计 (按维度聚合)
     *
     * @param from      时间起
     * @param to        时间止
     * @param dimension 维度
     * @param status    状态过滤 (可为空)
     * @param currency  币种过滤 (可为空)
     * @param top       Top N
     * @return 统计行
     */
    @GetMapping
    public ResponseEntity<Result<List<OrderStatsRowRespond>>> stats(@RequestParam("from") String from,
                                                                    @RequestParam("to") String to,
                                                                    @RequestParam("dimension") String dimension,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) String currency,
                                                                    @RequestParam(defaultValue = "100") int top) {
        LocalDateTime fromTime = requireDateTime(from, "from");
        LocalDateTime toTime = requireDateTime(to, "to");
        OrderStatsDimension dimensionEnum = parseEnum(dimension, OrderStatsDimension.class);
        if (dimensionEnum == null)
            throw new IllegalParamException("维度值不能为空");
        OrderStatus statusEnum = parseEnum(status, OrderStatus.class);
        OrderStatsQuery query = OrderStatsQuery.builder()
                .from(fromTime)
                .to(toTime)
                .dimension(dimensionEnum)
                .status(statusEnum)
                .currency(currency)
                .top(top)
                .build();
        query.validate();
        List<IAdminStatsService.OrderStatsRowView> rows = adminStatsService.stats(query);
        CurrencyConfig currencyConfig = query.getCurrency() == null ? null : currencyConfigService.get(query.getCurrency());
        List<OrderStatsRowRespond> data = rows.stream().map(r -> OrderStatsRowRespond.builder()
                .dimension(r.dimension())
                .keyId(parseLongOrNull(r.dimensionKey()))
                .keyCode(null)
                .keyName(null)
                .ordersCount(r.ordersCount() == null ? 0 : r.ordersCount().intValue())
                .itemsCount(r.itemsCount() == null ? null : r.itemsCount().intValue())
                .subtotalAmountSum(currencyConfig == null || r.totalAmountMinor() == null ? null : currencyConfig.toMajor(r.totalAmountMinor()).toPlainString())
                .payAmountSum(currencyConfig == null || r.payAmountMinor() == null ? null : currencyConfig.toMajor(r.payAmountMinor()).toPlainString())
                .discountAmountSum(currencyConfig == null || r.discountAmountMinor() == null ? null : currencyConfig.toMajor(r.discountAmountMinor()).toPlainString())
                .shippingAmountSum(currencyConfig == null || r.shippingAmountMinor() == null ? null : currencyConfig.toMajor(r.shippingAmountMinor()).toPlainString())
                .appliedAmountSum(null)
                .build()).toList();

        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 解析 ISO-8601 date-time 到 {@link LocalDateTime} (必填)
     *
     * @param value     字符串
     * @param fieldName 字段名
     * @return LocalDateTime
     */
    private static LocalDateTime requireDateTime(String value, String fieldName) {
        LocalDateTime t = parseDateTime(value);
        if (t == null)
            throw new IllegalParamException(fieldName + " 不能为空");
        return t;
    }

    /**
     * 解析 ISO-8601 date-time 到 {@link LocalDateTime}
     *
     * @param value 字符串 (可为空)
     * @return LocalDateTime 或 null
     */
    private static @Nullable LocalDateTime parseDateTime(@Nullable String value) {
        if (value == null || value.isBlank())
            return null;
        String trimmed = value.strip();
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception ex) {
                throw new IllegalParamException("时间格式不合法: " + value);
            }
        }
    }

    /**
     * 解析枚举 (字符串为空返回 null)
     *
     * @param value    字符串
     * @param enumType 枚举类型
     * @param <E>      枚举泛型
     * @return 枚举或 null
     */
    private static <E extends Enum<E>> @Nullable E parseEnum(@Nullable String value, Class<E> enumType) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Enum.valueOf(enumType, value.strip());
        } catch (Exception e) {
            throw new IllegalParamException("枚举值不合法: " + value);
        }
    }

    /**
     * 字符串 → Long (可为空)
     *
     * @param value 字符串
     * @return Long 或 null
     */
    private static Long parseLongOrNull(@Nullable String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Long.parseLong(value);
        } catch (Exception ignore) {
            return null;
        }
    }
}

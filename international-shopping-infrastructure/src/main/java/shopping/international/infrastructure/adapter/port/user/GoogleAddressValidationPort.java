package shopping.international.infrastructure.adapter.port.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import shopping.international.domain.adapter.port.user.IAddressValidationPort;
import shopping.international.domain.model.enums.user.AddressValidationStatus;
import shopping.international.domain.model.vo.user.AddressValidationCommand;
import shopping.international.domain.model.vo.user.AddressValidationResult;
import shopping.international.domain.model.vo.user.UserAddressExtension;
import shopping.international.infrastructure.gateway.user.GoogleAddressValidationApi;
import shopping.international.types.config.GoogleAddressValidationProperties;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * Google Address Validation 端口实现
 *
 * <p>职责:</p>
 * <ul>
 *     <li>将内部 {@link AddressValidationCommand} 转换为 Google Address Validation API 请求体</li>
 *     <li>解析 Google 返回的 {@code verdict/address/geocode} 结构, 产出内部 {@link AddressValidationResult}</li>
 *     <li>按照业务规则将 Google 的 {@code possibleNextAction} 与细粒度 verdict 映射为内部校验状态</li>
 *     <li>生成用于 {@code user_address_ext} 落库的扩展快照</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GoogleAddressValidationProperties.class)
public class GoogleAddressValidationPort implements IAddressValidationPort {

    /**
     * Google Address Validation API 的固定路径
     */
    private static final String VALIDATE_ADDRESS_PATH = "/v1:validateAddress";

    /**
     * JSON 序列化/反序列化器
     *
     * <p>用于解析 Google 响应, 以及将扩展快照序列化为 JSON 文本</p>
     */
    private final ObjectMapper objectMapper;
    /**
     * Google Address Validation Retrofit API 客户端
     */
    private final GoogleAddressValidationApi api;
    /**
     * Google Address Validation 配置
     *
     * <p>至少需要提供 baseUrl 与 apiKey</p>
     */
    private final GoogleAddressValidationProperties properties;

    /**
     * 调用 Google Address Validation 执行地址校验并映射为内部结果
     *
     * <p>处理流程:</p>
     * <ol>
     *     <li>校验本地配置与关键入参</li>
     *     <li>向 Google 发起校验请求</li>
     *     <li>解析响应中的规范化地址, verdict, possibleNextAction 与 geocode 信息</li>
     *     <li>映射为内部 {@link AddressValidationResult}</li>
     * </ol>
     *
     * @param command 地址校验命令, 包含待校验的结构化地址与可选的 Google 上下文信息
     * @return 规范化后的内部地址校验结果
     * @throws IllegalParamException 当配置缺失, 请求失败或响应无法解析时抛出
     */
    @Override
    public @NotNull AddressValidationResult validate(@NotNull AddressValidationCommand command) {
        requireConfigured();
        requireNotBlank(command.countryCode(), "国家编码不能为空");
        requireNotBlank(command.country(), "国家不能为空");
        requireNotBlank(command.addressLine1(), "地址行1不能为空");

        try {
            Response<ResponseBody> response = api.validateAddress(
                    properties.getBaseUrl() + VALIDATE_ADDRESS_PATH,
                    properties.getApiKey(),
                    buildRequest(command)
            ).execute();

            try (ResponseBody body = response.body(); ResponseBody errorBody = response.errorBody()) {
                if (!response.isSuccessful() || body == null)
                    throw new IllegalParamException("Google 地址校验失败, HTTP code: " + response.code()
                            + ", 错误体: " + (errorBody == null ? null : errorBody.string()));

                JsonNode root = objectMapper.readTree(body.bytes());
                JsonNode result = root.path("result");
                JsonNode verdict = result.path("verdict");
                JsonNode address = result.path("address");
                JsonNode postalAddress = address.path("postalAddress");
                JsonNode geocode = result.path("geocode");

                String countryCode = textOrNull(postalAddress, "regionCode");
                if (countryCode == null)
                    countryCode = command.countryCode();
                String languageCode = textOrNull(postalAddress, "languageCode");
                if (languageCode == null)
                    languageCode = command.languageCode();

                List<String> addressLines = textList(postalAddress.path("addressLines"));
                if (addressLines.isEmpty()) {
                    addressLines.add(command.addressLine1());
                    if (command.addressLine2() != null && !command.addressLine2().isBlank())
                        addressLines.add(command.addressLine2());
                }

                String addressLine1 = addressLines.get(0);
                String addressLine2 = addressLines.size() <= 1 ? null : String.join(", ", addressLines.subList(1, addressLines.size()));
                String possibleNextAction = textOrNull(verdict, "possibleNextAction");
                AddressValidationStatus validationStatus = mapValidationStatus(verdict, result, postalAddress, geocode, possibleNextAction, addressLine1);

                return new AddressValidationResult(
                        countryCode.toUpperCase(Locale.ROOT),
                        normalizeCountryName(countryCode, languageCode, command.country()),
                        firstNonBlank(textOrNull(postalAddress, "administrativeArea"), command.province()),
                        firstNonBlank(textOrNull(postalAddress, "locality"), command.city()),
                        firstNonBlank(textOrNull(postalAddress, "sublocality"), command.district()),
                        addressLine1,
                        firstNonBlank(addressLine2, command.addressLine2()),
                        firstNonBlank(textOrNull(postalAddress, "postalCode"), command.zipcode()),
                        languageCode,
                        validationStatus,
                        possibleNextAction,
                        buildExtension(command, root, result, verdict, address, postalAddress, geocode)
                );
            }
        } catch (IllegalParamException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalParamException("Google 地址校验异常: " + exception.getMessage(), exception);
        }
    }

    /**
     * 将内部地址校验命令转换为 Google Address Validation API 请求体
     *
     * <p>该方法会根据命令内容按需补充:</p>
     * <ul>
     *     <li>{@code regionCode / languageCode / addressLines}</li>
     *     <li>{@code administrativeArea / locality / sublocality / postalCode}</li>
     *     <li>若存在上一轮 {@code responseId}, 则带上 {@code previousResponseId}</li>
     *     <li>对 US/PR 自动开启 {@code enableUspsCass}</li>
     * </ul>
     *
     * @param command 地址校验命令
     * @return 可直接发送给 Google API 的请求体
     */
    private Map<String, Object> buildRequest(AddressValidationCommand command) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("regionCode", command.countryCode());
        if (command.languageCode() != null && !command.languageCode().isBlank())
            address.put("languageCode", command.languageCode());
        List<String> addressLines = new ArrayList<>();
        addressLines.add(command.addressLine1());
        if (command.addressLine2() != null && !command.addressLine2().isBlank())
            addressLines.add(command.addressLine2());
        address.put("addressLines", addressLines);
        if (command.province() != null && !command.province().isBlank())
            address.put("administrativeArea", command.province());
        if (command.city() != null && !command.city().isBlank())
            address.put("locality", command.city());
        if (command.district() != null && !command.district().isBlank())
            address.put("sublocality", command.district());
        if (command.zipcode() != null && !command.zipcode().isBlank())
            address.put("postalCode", command.zipcode());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("address", address);
        if (command.previousResponseId() != null && !command.previousResponseId().isBlank())
            body.put("previousResponseId", command.previousResponseId());
        if (Objects.equals(command.countryCode(), "US") || Objects.equals(command.countryCode(), "PR"))
            body.put("enableUspsCass", Boolean.TRUE);
        return body;
    }

    /**
     * 根据 Google 响应构建地址扩展快照
     *
     * <p>该扩展对象会被后续仓储层映射到 {@code user_address_ext} 表, 保存 Place/Validation/Geocode
     * 的附加信息以及原始输入上下文</p>
     *
     * @param command       原始校验命令
     * @param root          Google 完整响应根节点
     * @param result        响应中的 {@code result} 节点
     * @param verdict       响应中的 {@code verdict} 节点
     * @param address       响应中的 {@code address} 节点
     * @param postalAddress 响应中的标准化 {@code postalAddress} 节点
     * @param geocode       响应中的 {@code geocode} 节点
     * @return 可直接用于落库的地址扩展快照
     */
    private UserAddressExtension buildExtension(AddressValidationCommand command,
                                                JsonNode root,
                                                JsonNode result,
                                                JsonNode verdict,
                                                JsonNode address,
                                                JsonNode postalAddress,
                                                JsonNode geocode) {
        JsonNode location = geocode.path("location");
        return UserAddressExtension.builder()
                .googlePlaceId(firstNonBlank(command.googlePlaceId(), textOrNull(geocode, "placeId")))
                .formattedAddress(textOrNull(address, "formattedAddress"))
                .latitude(decimalOrNull(location, "latitude"))
                .longitude(decimalOrNull(location, "longitude"))
                .rawInput(command.rawInput())
                .postalAddressJson(toJsonOrNull(postalAddress))
                .placeResponseJson(toJsonOrNull(command.placeResponse()))
                .validationResponseId(textOrNull(root, "responseId"))
                .validationGranularity(textOrNull(verdict, "validationGranularity"))
                .geocodeGranularity(textOrNull(verdict, "geocodeGranularity"))
                .addressComplete(boolOrNull(verdict, "addressComplete"))
                .possibleNextAction(textOrNull(verdict, "possibleNextAction"))
                .validationResponseJson(toJsonOrNull(root))
                .build();
    }

    /**
     * 将 Google 的 verdict 与 possibleNextAction 映射为内部地址校验状态
     *
     * <p>映射优先级:</p>
     * <ol>
     *     <li>优先使用 {@code possibleNextAction}</li>
     *     <li>再根据是否明显无效判断是否应进入 {@link AddressValidationStatus#REJECT}</li>
     *     <li>最后结合 granularity, 完整性与轻微修正信号给出 {@code FIX/REVIEW/ACCEPT}</li>
     * </ol>
     *
     * @param verdict            Google 返回的 verdict 节点
     * @param result             Google 返回的 result 节点
     * @param postalAddress      Google 规范化后的 postalAddress 节点
     * @param geocode            Google 返回的 geocode 节点
     * @param possibleNextAction Google 建议的下一步动作, 可为空
     * @param addressLine1       当前已推导出的地址行 1, 可为空
     * @return 内部地址校验状态
     */
    private AddressValidationStatus mapValidationStatus(JsonNode verdict,
                                                        JsonNode result,
                                                        JsonNode postalAddress,
                                                        JsonNode geocode,
                                                        @Nullable String possibleNextAction,
                                                        @Nullable String addressLine1) {
        if ("ACCEPT".equals(possibleNextAction))
            return AddressValidationStatus.ACCEPT;
        if ("CONFIRM".equals(possibleNextAction) || "CONFIRM_ADD_SUBPREMISES".equals(possibleNextAction))
            return AddressValidationStatus.REVIEW;
        if ("FIX".equals(possibleNextAction))
            return isHardReject(result, postalAddress, geocode, addressLine1) ? AddressValidationStatus.REJECT : AddressValidationStatus.FIX;

        if (isHardReject(result, postalAddress, geocode, addressLine1))
            return AddressValidationStatus.REJECT;

        String validationGranularity = textOrNull(verdict, "validationGranularity");
        boolean addressComplete = Boolean.TRUE.equals(boolOrNull(verdict, "addressComplete"));
        boolean hasMinorIssues = Boolean.TRUE.equals(boolOrNull(verdict, "hasInferredComponents"))
                || Boolean.TRUE.equals(boolOrNull(verdict, "hasReplacedComponents"))
                || Boolean.TRUE.equals(boolOrNull(verdict, "hasSpellCorrectedComponents"))
                || Boolean.TRUE.equals(boolOrNull(verdict, "hasUnconfirmedComponents"));
        boolean onlyMissingSubpremise = onlyMissingSubpremise(result.path("address").path("missingComponentTypes"));

        if ("OTHER".equals(validationGranularity) || "ROUTE".equals(validationGranularity) || !addressComplete)
            return AddressValidationStatus.FIX;
        if ("PREMISE_PROXIMITY".equals(validationGranularity) || hasMinorIssues || onlyMissingSubpremise)
            return AddressValidationStatus.REVIEW;
        return AddressValidationStatus.ACCEPT;
    }

    /**
     * 判断当前 Google 校验结果是否属于“明显无效/不可投递”的硬拒绝场景
     *
     * <p>典型场景包括:</p>
     * <ul>
     *     <li>缺少结构化地址核心要素, 无法形成可投递地址</li>
     *     <li>粒度极低且无 geocode 命中, 同时存在 unresolved tokens</li>
     * </ul>
     *
     * @param result        Google 返回的 result 节点
     * @param postalAddress Google 规范化后的 postalAddress 节点
     * @param geocode       Google 返回的 geocode 节点
     * @param addressLine1  已推导出的地址行 1, 可为空
     * @return 若应直接拒绝则返回 {@code true}
     */
    private boolean isHardReject(JsonNode result, JsonNode postalAddress, JsonNode geocode, @Nullable String addressLine1) {
        boolean missingStructuredAddress = textOrNull(postalAddress, "regionCode") == null
                || addressLine1 == null || addressLine1.isBlank();
        boolean hasNoGeocode = geocode.isMissingNode() || geocode.isNull() || geocode.path("location").isMissingNode();
        boolean hasUnresolvedTokens = result.path("address").path("unresolvedTokens").isArray()
                && !result.path("address").path("unresolvedTokens").isEmpty();
        String validationGranularity = textOrNull(result.path("verdict"), "validationGranularity");
        return missingStructuredAddress || ("OTHER".equals(validationGranularity) && hasNoGeocode && hasUnresolvedTokens);
    }

    /**
     * 判断缺失组件是否仅为 {@code subpremise}
     *
     * <p>若只缺少子单元信息, 通常更适合提示用户复核而不是直接判为无效</p>
     *
     * @param missingComponentTypes Google 返回的 {@code missingComponentTypes} 节点
     * @return 若仅缺少 {@code subpremise} 则返回 {@code true}
     */
    private boolean onlyMissingSubpremise(JsonNode missingComponentTypes) {
        if (!missingComponentTypes.isArray() || missingComponentTypes.isEmpty())
            return false;
        for (JsonNode node : missingComponentTypes) {
            if (!"subpremise".equalsIgnoreCase(node.asText()))
                return false;
        }
        return true;
    }

    /**
     * 校验 Google Address Validation 所需配置是否齐全
     *
     * @throws IllegalParamException 当 {@code baseUrl} 或 {@code apiKey} 缺失时抛出
     */
    private void requireConfigured() {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()
                || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalParamException("Google Address Validation 未配置, 请设置 google.address-validation.base-url 和 api-key");
        }
    }

    /**
     * 根据国家代码与语言标签生成标准化国家名称
     *
     * <p>若无法生成可读国家名, 则回退到调用方传入的 fallback</p>
     *
     * @param countryCode  国家/地区代码
     * @param languageCode 地址语言标签, 可为空
     * @param fallback     兜底国家名称, 可为空
     * @return 标准化国家名称
     */
    private String normalizeCountryName(String countryCode, @Nullable String languageCode, @Nullable String fallback) {
        Locale displayLocale = languageCode == null || languageCode.isBlank()
                ? Locale.ENGLISH
                : Locale.forLanguageTag(languageCode);
        String displayCountry = new Locale("", countryCode).getDisplayCountry(displayLocale);
        return displayCountry.isBlank() ? fallback : displayCountry;
    }

    /**
     * 返回第一个非空白字符串, 否则回退到 fallback
     *
     * @param first    优先值, 可为空
     * @param fallback 兜底值, 可为空
     * @return 选中的字符串, 两者均为空白时返回 fallback
     */
    private @Nullable String firstNonBlank(@Nullable String first, @Nullable String fallback) {
        if (first != null && !first.isBlank())
            return first;
        return fallback;
    }

    /**
     * 从 JSON 节点中读取指定文本字段
     *
     * @param node      源 JSON 节点
     * @param fieldName 字段名
     * @return 若字段存在且非 null 则返回其文本值, 否则返回 {@code null}
     */
    private @Nullable String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    /**
     * 将 JSON 数组节点转换为字符串列表
     *
     * <p>会自动过滤 null 和空白元素</p>
     *
     * @param node 源 JSON 节点
     * @return 转换后的字符串列表; 若不是数组则返回空列表
     */
    private List<String> textList(JsonNode node) {
        if (!node.isArray())
            return List.of();
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isNull()) {
                String value = item.asText();
                if (!value.isBlank())
                    values.add(value);
            }
        }
        return values;
    }

    /**
     * 从 JSON 节点中读取布尔字段
     *
     * @param node      源 JSON 节点
     * @param fieldName 字段名
     * @return 若字段存在则返回其布尔值, 否则返回 {@code null}
     */
    private @Nullable Boolean boolOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asBoolean();
    }

    /**
     * 从 JSON 节点中读取十进制字段
     *
     * @param node      源 JSON 节点
     * @param fieldName 字段名
     * @return 若字段存在则返回其十进制值, 否则返回 {@code null}
     */
    private @Nullable BigDecimal decimalOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.decimalValue();
    }

    /**
     * 将任意对象序列化为 JSON 字符串
     *
     * @param value 待序列化对象, 可为空
     * @return 序列化后的 JSON 文本; 若入参为空则返回 {@code null}
     * @throws IllegalParamException 当序列化失败时抛出
     */
    private @Nullable String toJsonOrNull(@Nullable Object value) {
        if (value == null)
            return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalParamException("序列化地址扩展快照失败: " + exception.getMessage(), exception);
        }
    }
}

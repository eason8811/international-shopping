# AGENTS – `international-shopping` 后端

## 1. Project Overview / Scope

### 1.1 项目简介

* **系统名称**：International Shopping（国际购物网站后端）
* **定位**：提供多语言、多币种、第三方登录能力的电商后端服务。
* **主要职责（按领域划分）**：

  - **用户领域（user）**  
    基于 `user_account / user_auth / user_profile / user_address` 等表，负责：
      - 维护本地用户账户（用户名、邮箱、手机号登录唯一性、账号状态、最近登录时间）；
      - 管理本地密码与第三方 OAuth2 认证映射（Google / TikTok / X 等），支持多通道绑定与登录；
      - 存储用户扩展资料（头像、性别、生日、地区、个性化扩展字段）；
      - 管理用户收货地址（多地址、默认地址、国际化地址结构），为下单与物流发货提供地址基础数据。

  - **商品领域（product）**  
    基于 `product_category / product_category_i18n / product / product_i18n / product_sku / product_image / product_sku_image / product_spec* / product_price / product_like / product_sku_spec` 等表，负责：
      - 维护商品分类树及其多语言信息（分类名、slug、品牌文案），支持 SEO 和多语言路由；
      - 管理商品 SPU 及其多语言文案（标题、副标题、描述、路由 slug、标签）；
      - 管理商品 SKU（库存、启用状态、条码、重量）及 SPU / SKU 图片集（主图、多图、变体专图）；
      - 定义规格类别与规格值（颜色、容量、材质等），以及 SKU 与规格值的映射，用于前端多规格选型；
      - 维护 SKU 多币种价格（标价、促销价、启用状态），支撑多币种结算与展示；
      - 记录用户对商品的 Like 关系，用于个性化推荐与社交化运营。

  - **订单领域（orders）**  
    基于 `orders / order_item / order_status_log / inventory_log / shopping_cart_item / discount_policy / discount_code* / order_discount_applied` 等表，负责：
      - 生成并维护订单主档（订单号、金额构成、币种、支付状态、地址快照、买家备注、取消原因等）；
      - 记录订单明细（关联 SPU/SKU、标题快照、属性快照、单价/小计）；
      - 追踪订单状态流转日志（来源、状态机 from→to），用于审计与状态回溯；
      - 记录库存变动日志（预占、扣减、释放、入库），支撑库存审计与问题排查；
      - 管理购物车条目（用户-SKU 映射、数量、是否勾选），为下单流程提供输入；
      - 维护折扣策略与折扣码（策略模板、折扣码、适用范围映射），并记录折扣实际应用流水（订单级/明细级），用于对账与运营分析。

  - **支付领域（payment）**  
    基于 `payment_order / payment_refund / payment_refund_item` 等表，负责：
      - 为订单生成支付单（关联订单、支付通道、网关 externalId、状态、请求/响应/回调报文），对接支付宝 / 微信 / Stripe / PayPal 等支付网关；
      - 维护支付状态机及轮询/回调相关时间戳，用于支付结果确认与重试调度；
      - 管理退款单（退款号、金额拆分、原因、发起方、externalId、状态），实现与支付网关的退款对账与幂等控制；
      - 按订单明细拆分退款明细（数量、金额、原因），支持部分退款与精细化对账。

  - **物流领域（shipping）**  
    基于 `shipment / shipment_item / shipment_status_log / shipping_claim` 等表，负责：
      - 管理跨境物流包裹（内部包裹号、承运商编码与名称、追踪号、尺寸重量、申报价值、关务信息、面单 URL 等）；
      - 记录包裹状态机及其流转日志（CARRIER_WEBHOOK / POLL / SYSTEM_JOB / MANUAL / API 等来源），用于轨迹回放与看板统计；
      - 维护包裹与订单明细的 N:N 映射（支持合单发货与拆单发货、部分发货），为订单履约与售后提供依据；
      - 管理承运商理赔单（原因、状态、理赔金额、已支付金额、关联订单/包裹/补发单/工单），实现与物流侧的赔付对账。

  - **售后领域（customerservice）**  
    基于 `cs_ticket / aftersales_reship / aftersales_reship_item / aftersales_reship_shipment` 等表，负责：
      - 提供统一的客服工单入口（退款、补寄、理赔、物流问题、改址、商品问题、支付异常等），并维护工单状态机、优先级、SLA、指派信息与多维关联（用户/订单/明细/包裹）；
      - 记录工单描述、附件、举证材料、标签等信息，支撑客服处理与运营分析；
      - 管理售后补发单（补发原因、状态、成本、备注），并基于原订单明细生成补发表，支持按明细粒度补发；
      - 维护补发单与物流包裹的关联，为补发流程与理赔流程打通数据链路。

  - **基础字典 / 站点配置领域（supporting dictionaries）**  
    基于 `currency / locale` 等表，负责：
      - 维护币种字典（ISO 4217 代码、名称、符号、最小货币单位、现金舍入规则、启用状态），为多币种定价、结算与金额展示提供标准化配置；
      - 维护站点可用语言（语言代码、名称、本地名、启用状态、全站唯一默认语言），支撑商品/分类/规格等多语言文案映射与路由策略。

### 1.2 代码范围（Scope）

**AGENT 主要负责的目录 / 模块：**

* Maven 模块（命名以 `shopping.international-` 开头）：

    * `shopping.international-api`：公共 API 模型 / DTO / 统一响应结构。
    * `shopping.international-types`：跨模块通用类型、异常、工具类（不可依赖业务模块）。
    * `shopping.international-domain`：领域模型与领域服务（DDD 核心：聚合、实体、值对象、仓储接口、领域服务）。
    * `shopping.international-app`：应用服务 / 用例编排层（调用 domain、组合多个聚合 / 端口、Web 安全配置、CSRF 校验策略等）。
    * `shopping.international-infrastructure`：基础设施适配层（数据库、Redis、第三方服务、后续可能有消息队列等 port 的实现）。
    * `shopping.international-trigger`：触发层（REST Controller、OAuth2 回调等）。

**AGENT 可以修改的内容（优先）：**

* 以上模块下的 Java 代码、测试代码。
* 与该服务直接相关的配置：

    * `src/main/resources/application.yml` / `src/main/resources/application-*.yml` / `.properties`
    * 与 international-shopping 直接关联的 `docker-compose-*.yaml`（**仅限开发环境**）。

**AGENT 不应主动修改的内容（除非明确说明）：**

* 生产环境部署脚本 / 运维仓库（如 `docker-compose-prod.yaml`）。

---

## 2. Tech Stack & Important Paths

### 2.1 技术栈

* **语言 & 运行环境**

    * Java 17
    * Maven（推荐使用 `./mvnw` 包装器，如存在）

* **框架**

    * Spring Boot 3.x
    * Spring MVC / Spring Web
    * Spring Security（JWT + CSRF 双提交 Cookie 策略）
    * Spring OAuth2 Client（用于 Google / X / TIKTOK 等第三方登录）
    * MyBatis-Plus（数据访问）
    * Spring Data Redis / Redisson（如 Redis 幂等性、分布式锁等）

* **中间件**

    * MySQL 8.x（主库）
    * Redis（缓存、幂等性、验证码存储等）

### 2.2 核心包结构（Java）

* `shopping.international.api`

    * `shopping.international.api.req.*`：trigger 层用到的请求 DTO
    * `shopping.international.api.resp.*`：trigger 层用到的响应 DTO（如 `UserAccountRespond`）
    * `shopping.international.api.resp.Result`：系统对外提供服务接口的统一返回结构

* `shopping.international.types`

    * `shopping.international.types.exceptions.*`：如 `IllegalParamException` 等自定义异常
    * 通用 枚举类 / 工具类（与业务无关的跨领域工具）

* `shopping.international.domain`

    * `shopping.international.domain.model.aggregate.user.*`：用户（user）领域 User 聚合根，用于关联UserProfile、UserAddress、AuthBinding 等实体或 vo。`shopping.international.domain.model.aggregate` 包是专门存放领域聚合根的包，目前只有 `shopping.international.domain.model.aggregate.user` 未来需要新增领域时需要在该包下新增以领域名命名的子包，如 `shopping.international.domain.model.aggregate.product` 。
    * `shopping.international.domain.model.entity.user.*`：用户（user）领域实体，如 UserAddress、AuthBinding 等。`shopping.international.domain.model.entity` 包是专门存放领域实体的包，目前只有 `shopping.international.domain.model.entity.user` 未来需要新增领域时需要在该包下新增以领域名命名的子包，如 `shopping.international.domain.model.entity.orders` 。
    * `shopping.international.domain.model.vo.user.*`：用户（user）领域值对象，如 `PhoneNumber` 等。`shopping.international.domain.model.vo` 包是专门存放领域值对象的包，目前只有 `shopping.international.domain.model.vo.user` 未来需要新增领域时需要在该包下新增以领域名命名的子包，如 `shopping.international.domain.model.vo.payment` 。
    * `shopping.international.domain.model.enums.*`：用户（user）领域枚举，如 `AccountStatus` 等。`shopping.international.domain.model.enums` 包是专门存放领域枚举的包，目前只有 `shopping.international.domain.model.enums.user` 未来需要新增领域时需要在该包下新增以领域名命名的子包，如 `shopping.international.domain.model.enums.shipping` 。
    * `shopping.international.domain.adapter`：domain层的适配器，提供接口，供 infrastructure 实现，形成依赖倒置，让 domain 层不依赖于具体实现。
    * `shopping.international.domain.adapter.repository.user.*`：用户（user）领域仓储接口，如 `IUserRepository` 等，专门用于进行数据库持久化操作。`shopping.international.domain.adapter.repository` 包是专门存放仓储接口的包，目前只有 `shopping.international.domain.adapter.repository.user` 未来需要新增领域时需要在该包下新增以领域名命名的子包，如 `shopping.international.domain.adapter.repository.customerservice` 。
    * `shopping.international.domain.adapter.port.user.*`：用户（user）领域 Port 接口，如 `IUserAuthService` 等，专门用于与外部系统交互，包括请求外部系统API，或缓存（如 Redis）等。`shopping.international.domain.adapter.port` 包是专门存放 Port 接口的包，目前只有 `shopping.international.domain.adapter.port.user` 未来需要新增领域时需要在该包下新增以领域名命名的子包，如 `shopping.international.domain.adapter.port.shipping` 。
    * `shopping.international.domain.adapter.event.*`：用户（user）领域事件，目前还未建立具体的类，用于存放与发布订阅相关的接口（如消息队列等）。
    * `shopping.international.domain.service.*`：领域服务接口，具体领域具体分包，如`shopping.international.domain.service.user`专门用于用户领域的服务。
    * `shopping.international.domain.service.*.impl.*`：领域服务实现，具体领域具体分包，如`shopping.international.domain.service.user.impl`专门用于用户领域的服务实现。

- `shopping.international.app`  
  应用层与运行时“壳”模块，承载跨领域的 Spring 配置、AOP 切面、安全相关组件以及全局异常翻译等，没有具体的业务逻辑。

    - `shopping.international.app.config`  
      应用级配置与基础设施装配入口，例如：
        - Spring Boot / Spring MVC / Jackson 等全局配置；
        - 安全相关的 Bean 装配（认证管理器、密码编码器等，与 `trigger` 模块的 `SecurityConfig` 协同）；
        - 将领域层 Port 接口与具体实现进行装配（通过构造器注入或 `@Bean` 暴露）。

    - `shopping.international.app.aop`  
      AOP 切面与横切关注点

    - `shopping.international.app.handler`  
      全局级别的处理器（Handler），主要负责：
        - 将领域层 / 应用层抛出的业务异常、校验异常，转换为统一的 `Result` 返回结构；
        - 对部分框架异常（如认证失败、参数绑定失败）进行统一封装，避免在每个 Controller 重复处理；
        - 作为触发层与领域/应用层之间的“错误边界”，保证对外返回格式一致。

    - `shopping.international.app.security`  
      安全相关的应用层组件，围绕 Spring Security 做集成与适配：

        - `shopping.international.app.security.filter`  
          请求过滤器，例如：
            - 解析并验证 JWT 的过滤器，将认证信息写入 `SecurityContext`；
            - 与 CSRF / 双提交 Cookie 策略配合的自定义过滤器（如有）。

        - `shopping.international.app.security.handler`  
          安全事件处理器，例如：
            - 未认证 / 无权限时的统一响应（`AuthenticationEntryPoint` / `AccessDeniedHandler`）；
            - 登录成功 / 失败、OAuth2 成功 / 失败回调等 Handler，将安全事件包装为统一 API 响应或重定向。

        - `shopping.international.app.security.service`  
          安全相关的应用服务，例如：
            - 用户详情加载 / 权限装配服务（`UserDetailsService` 等）；
            - Token 生成与校验的封装服务；
            - 在需要访问业务数据时，通过 **领域 Port 接口** 间接访问用户 / 权限信息，**不直接依赖基础设施实现**。

* `shopping.international.infrastructure`

  基础设施层，实现 domain 层定义的各类 Port / Repository / Event 适配器，并封装具体的技术细节（数据库、Redis、HTTP 客户端等）。**只依赖 domain 暴露的接口，不被 domain 依赖**。

    * `shopping.international.infrastructure.adapter.*`：基础设施适配器总包，实现 domain 层的 adapter 接口（依赖倒置的“实现方”）。

        * `shopping.international.infrastructure.adapter.repository.user.*`：用户（user）领域仓储接口的实现，如 `UserRepository` 等。  
          主要职责：
            - 实现 `shopping.international.domain.adapter.repository.user.*` 中定义的仓储接口（如 `IUserRepository`）；
            - 通过 `shopping.international.infrastructure.dao.user*` 中的 PO / Mapper 完成用户账户、认证映射、资料、地址等数据的持久化与查询；
            - 负责在 PO ↔ 聚合 / 实体之间做映射转换。  
              `shopping.international.infrastructure.adapter.repository` 包是专门存放“仓储实现”的包，目前只有 `user` 领域，未来其他领域（如 `product`、`orders`）可以在该包下新增子包：`shopping.international.infrastructure.adapter.repository.product` 等。

        * `shopping.international.infrastructure.adapter.port.user.*`：用户（user）领域 Port 接口的实现，如 Redis 幂等性、验证码存储、第三方服务调用等。  
          主要职责：
            - 实现 `shopping.international.domain.adapter.port.user.*` 中定义的 Port 接口（如 `IAddressIdempotencyPort` 等）；
            - 封装对 Redis、消息队列、HTTP/REST 客户端等外部基础设施的具体操作；
            - 保证上层（domain/app）只依赖 Port 接口，不关心具体技术选型。  
              `shopping.international.infrastructure.adapter.port` 包是专门存放 Port 实现的包，目前只有 `user` 领域，未来可以新增如 `shopping.international.infrastructure.adapter.port.shipping` 等子包。

        * `shopping.international.infrastructure.adapter.event.*`：领域事件相关适配器，目前可用于放置“事件发布/订阅”的基础设施实现（如 MQ、Spring 事件、Webhook 推送等）。
            - 将 domain 层的事件接口适配到底层消息中间件；
            - 负责事件序列化、Topic/RoutingKey 映射等技术细节。  
              目前尚未有具体实现类，后续接入消息系统时在该包下扩展。

    * `shopping.international.infrastructure.dao.*`：数据访问对象（DAO）层，贴近数据库表结构的“持久化模型”。

        * `shopping.international.infrastructure.dao.user.po.*`：用户（user）领域相关的持久化对象（PO），与 `user_account / user_auth / user_profile / user_address` 等表结构高度对应。  
          主要职责：
            - 承载 MyBatis / MyBatis-Plus 的实体映射（字段基本一一对应数据库列）；
            - 不包含业务逻辑，仅作为持久化载体；
            - 通常由仓储实现层转换为领域聚合/实体使用。  
              `shopping.international.infrastructure.dao` 包是专门存放各领域的 PO（以及对应 Mapper）的包，目前只有 `user`，未来其他领域可以在该包下新增子包，如 `shopping.international.infrastructure.dao.product.po`。

    * `shopping.international.infrastructure.gateway.*`：对外部系统（第三方服务、网关）的“网关层”封装，作为 HTTP/RPC 客户端的统一出口。

        * `shopping.international.infrastructure.gateway.user.dto.*`：用户（user）领域相关的外部接口 DTO，例如调用第三方用户 / OAuth2 / Profile 等 API 时使用的请求/响应模型。  
          主要职责：
            - 描述外部系统的字段结构，与 domain 模型解耦；
            - 做好与外部 API 的版本、字段差异的适配；
            - 通常由 Port 实现层调用 gateway，并在 DTO ↔ 领域模型之间做转换。  
              `shopping.international.infrastructure.gateway` 包是专门存放外部系统网关封装的包，目前以 `user` 为主，未来可以扩展如 `shopping.international.infrastructure.gateway.payment`、`...gateway.shipping` 等。

  > 约定：`shopping.international.infrastructure` 只作为 **domain adapter 的实现层** 使用，与 domain 层的通信应该只依赖 domain 层的接口和聚合根，实体或值对象；

* `shopping.international.trigger`

    * `shopping.international.trigger.controller.*`：REST Controller（如 `AuthController`, `AddressController` 等）。根据领域分包，目前只有 `shopping.international.trigger.controller.user` 未来需要新增领域时需要在 `shopping.international.trigger.controller` 包下新增以领域名命名的子包，如 `shopping.international.trigger.controller.orders` 。
    * `shopping.international.trigger.job.*`：Spring 定时任务。根据领域分包，目前为空，未来需要新增领域时需要在 `shopping.international.trigger.job` 包下新增以领域名命名的子包，如 `shopping.international.trigger.job.orders` 。
    * `shopping.international.trigger.listener.*`：事件监听，如监听消息队列的消息。根据领域分包，目前为空，未来需要新增领域时需要在 `shopping.international.trigger.listener` 包下新增以领域名命名的子包，如 `shopping.international.trigger.listener.shipping` 。

    > 约定：`shopping.international.trigger` 中的组件在需要访问业务数据时，应优先依赖领域层定义的 service 领域服务接口接口，由 `domain` 层提供具体实现；禁止在此模块直接注入具体的 Mapper / Repository 实现或底层客户端（如直接操作 MyBatis-Plus Mapper、RedisTemplate 等），也不要把本层的与 HTTP 相关的对象 （如 Request、Respond等）直接透传给 `domain` 层，与 `domain` 层的通信应该依赖于领域服务接口和领域内的聚合根，实体或值对象。

### 2.3 重要配置路径

* Spring Boot 配置：

    * `src/main/resources/application.yml`
    * `src/main/resources/application-*.yml`（如 `application-dev.yml`）
* 安全配置：

    * `shopping.international.app.config.SecurityConfig`
    * `shopping.international.app.security.filter.CookieJwtAuthenticationFilter`
    * `shopping.international.app.security.handler.RestAuthErrorHandlers`
    * `shopping.international.app.security.handler.RestLogoutSuccessHandler`
* OAuth2 Provider 配置：

    * `application.yml` 中 `oauth2.auth-provider-properties-list` 等配置段。

---

## 3. Setup / Build / Run / Test Commands

### 3.1 依赖准备

* 必须安装：

    * JDK 17+
    * Maven 3.8+（或者使用仓库中的 `./mvnw`）
* 数据库：

    * 准备好本地或 Docker 版 MySQL & Redis（端口、账户在 `application-*.yml` 中配置）。
    * 首次运行前需要执行初始化 DDL（用户、商品、订单等表结构），位置通常为：

        * `dev-ops/dev/international-shopping.sql` 下的 SQL 文件。

### 3.2 编译 / 格式校验

```bash
# 编译所有模块（推荐先跑一遍）
./mvnw clean compile

# 只编译 international-shopping 相关模块（示例）
./mvnw -pl shopping.international-trigger -am compile
```

### 3.3 运行服务（开发模式）

Spring Boot 启动类在的位置是 `shopping.international.InternationalShoppingApplication`

```bash
# 运行后端服务（使用 dev 配置）
./mvnw -pl shopping.international -am spring-boot:run \
  -Dspring-boot.run.profiles=dev
```

### 3.4 测试命令

#### 全量测试

```bash
# 跑整个 multi-module 工程的测试
./mvnw test
```

#### 其他单测或集成测试命令可以自行补充

---

## 4. Code Style / Architecture Rules

### 4.1 分层 / 依赖方向（DDD + 分层架构）

**硬性规则：**

1. **依赖方向**（项目内模块禁止反向依赖）：

    * `trigger → app → domain → types`
    * `infrastructure → domain`（实现 domain 的 adapter 中的接口）
    * `domain` 只能依赖 `types`，**不能**依赖 `trigger` / `infrastructure` / `app`。
    * `trigger` 不得直接依赖 `infrastructure`，所有业务操作要走 `domain` 服务。

2. **Port / Adapter 规则**：

    * 所有对Redis、外部 HTTP 服务、以及API接口的访问，都通过 **Port 接口**（定义在 `shopping.international.domain.adapter.port.*`）暴露，在 `shopping.international.infrastructure.adapter.port.*` 中提供实现。对于使用HTTP访问外部系统API接口的场景，使用 Retrofit2 API来进行访问，相关的DTO（xxxRequest/xxxResponse）存放在 `shopping.international.infrastructure.gateway.*.dto` 中，API 接口存放在 `shopping.international.infrastructure.gateway.*`，并需要在 `shopping.international.app.config.RetrofitConfig` 中进行注册
    * 所有对持久化数据库（目前为MySQL）的访问，都通过 **Repository 接口**（定义在 `shopping.international.domain.adapter.repository.*`）暴露，在 `shopping.international.infrastructure.adapter.repository.*` 中提供实现。MyBatis-Plus 的 Mapper 和相关 PO 分别存放在 `shopping.international.infrastructure.dao.*` 和 `shopping.international.infrastructure.dao.*.po`，同样的分领域分包储存。
    * 新增调用外部系统需求时：

        * 先在 `domain` 中定义 Port 接口（语义化方法名，如 `IAddressIdempotencyPort.markPending`）。
        * 再在 `infrastructure` 中实现（如 `RedisAddressIdempotencyPort`）。

3. **聚合根与不变量**：

    * 聚合根（如 `User`）拥有自己的不变量与业务方法（如 `user.updateProfile(newProfile)`）。
    * **禁止**在聚合外部直接篡改聚合内部集合：

        * 不允许暴露 `List` 并让调用方 `add/remove`，而是通过聚合方法（例如 `user.addAddress(...)`）。
    * PATCH 语义的更新（部分字段更新）由聚合方法内部处理：

        * 传入的 VO 中为 `null` 的字段，不修改聚合中的对应字段。
        * 传入非 `null` 的字段才更新（如 `User#updateProfile(UserProfile newProfile)` 内部进行“null 忽略”合并）。

4. **异常处理**：

    * 参数校验失败 / 业务前置条件不满足：使用 `IllegalParamException.of("message")` 或专用业务异常。
    * 不要在领域层依赖 Spring Web 的异常类型（如 `ResponseStatusException`），这类异常应在 trigger 或 app 层负责转换。
    * 领域层异常应为“可预期业务异常”，用于提示上层做友好错误返回。
    * 当前的自定义异常无法描述当前异常时，可以新增自定义异常，并在全局异常处理器中捕获

5. **日志（Logging）**：

    * 使用 `Slf4j` Logger（`log.info/warn/error/debug`），禁止 `System.out.println`。
    * 日志内容尽量结构化，包含关键参数，如：`userId`, `orderId`, `addressId`, `provider` 等。
    * **不得在日志中输出敏感信息**：

        * 密码、验证码、JWT、OAuth2 access_token / refresh_token、邮件验证 token 等。
    * 错误日志请附带异常堆栈：`log.error("xxx failed, userId={}", userId, ex);`

6. **命名规范**：

    * Java 类 / 接口使用标准驼峰命名：

        * 配置类：`xxxConfig` 。
        * 配置属性类：`xxxProperties` 。
        * 聚合根：`User`, `Address`, `ProjectSnapshotAggregate` 等。
        * Port 接口：`IXXXPort`（如 `IAddressIdempotencyPort`）。
        * Port 实现：`XXXPort`（如 `OAuth2RemotePort`）。
        * 仓储接口：`IXXXRepository`（如 `IUserRepository`）。
        * 仓储实现：`XXXRepository`（如 `UserRepository`）。
        * 领域服务接口：`IXXXService`（如 `IUserService`）。
        * 领域服务实现：`XXXService`（如 `UserService`）。
        * 用于数据库持久化的PO对象：`XXXPO`（如 `UserAuthPO`）。
        * Mybatis-Plus Mapper：`XXXMapper`（如 `UserAuthMapper`）。
        * 领域事件：`XXXEvent`（如 `UserCreatedEvent`）。
        * `shopping.international.infrastructure.adapter.event` 适配器中的事件相关接口和实现类：接口名以 `IXXXEvent`，实现类以 `xxxEvent`。
        * trigger 层的 `shopping.international.trigger.job` 包下的定时任务： `XXXJob`。
        * trigger 层的 `shopping.international.trigger.listener` 包下的消息监听器： `XXXListener`。
    * Controller 命名：

        * `UserController`, `AddressController`, `AuthController` 等。
    * Request/Response DTO：

        * `CreateAddressRequest`, `UserAccountRespond` 等。
    * Gateway DTO 访问外部系统的 DTO
      * `xxxRequest`, `xxxxRespond` 等。
    * 包名全部小写，无下划线：`shopping.international.domain.model.vo.user`。

### 4.2 安全 / 鉴权规则

1. **JWT 与安全上下文**：

    * 所有需要登录用户身份的接口（如用户资料修改、地址增删改）必须从 `SecurityContext` 中获取 userId，而不是从请求参数中传 userId。
    * 获取当前用户 ID 的逻辑应抽取为公共工具（trigger 层），避免每个 Controller 都复制粘贴。

2. **CSRF（双提交 Cookie 模式）**：

    * 对所有“变更类接口”（POST/PUT/PATCH/DELETE）：

        * 前端必须带上 `X-CSRF-Token` 请求头；
        * 同时浏览器中有 `csrf_token` Cookie；
        * 两者必须相等才放行（由 Spring Security + `CookieCsrfTokenRepository` 自动处理）。
    * CSRF Token 的下发与轮换通过 `GET /auth/csrf`（`AuthController`）完成：

        * 响应体中回显 token；
        * 同时通过 `Set-Cookie` 设置 `csrf_token`。

3. **OAuth2 登录 / 绑定**：

    * 状态相关参数（`state`, `nonce`, `code_verifier` 等）必须由服务端生成并校验，避免前端绕过。
    * OAuth2 流程共用的逻辑（校验 state、交换 code、获取 userinfo 等）应抽取为可复用服务，而不是在多个领域服务中复制。

---

## 5. Testing Strategy

> 目标：对**领域逻辑**、**安全逻辑**、**变更类接口**提供可靠的回归保护。

### 5.1 测试框架

* 使用 JUnit 5（`spring-boot-starter-test`）。
* 建议对不同层使用不同风格：

    * 领域层：纯单元测试（不依赖 Spring 容器），只测试聚合 / 值对象 / 领域服务。
    * 应用 & 触发层：Spring Boot 测试（`@SpringBootTest` / `@WebMvcTest`），测试接口行为与安全配置。

### 5.2 必测区域

1. **安全与认证**：

    * JWT 鉴权过滤逻辑：

        * 带合法 JWT 时能成功访问需要授权的接口；
        * 不带或带无效 JWT 时拒绝访问。
    * CSRF 防护：

        * 增删改接口：无 CSRF Token / 不匹配时应被拒绝；
        * 正确带上 Cookie 与 Header 时放行。
    * OAuth2 回调：

        * state 不匹配时拒绝；
        * code 交换失败时正确处理错误；
        * 成功时能正确创建 / 绑定第三方账号记录。

2. **变更类接口测试用例（最低要求）**

对于每个“变更类接口”（POST/PUT/PATCH/DELETE），建议至少覆盖：

* **正向用例**：

    * 参数合法 + 授权合法 + CSRF 合法 → 成功修改（检查数据库变更 / 响应体）。
* **参数非法用例**：

    * 必填字段缺失 / 格式错误 → 返回统一错误结构，提示字段问题。
* **鉴权 / 授权用例**：

    * 未登录访问 → 返回未认证或未授权错误。
    * 登录了但访问他人资源（如修改他人地址） → 返回权限不足错误。
* **CSRF 用例**：

    * 无 `X-CSRF-Token` 头；
    * 头部与 Cookie 不一致；
    * 正确的双提交模式。
* **幂等性用例（如适用）**：

    * 同一个幂等键重复请求 → 不重复执行副作用，结果可重复。

1. **回归保护**

* 当对领域模型 / 安全配置 / 核心接口进行修改时：

    * 至少新增或更新对应测试；
    * 在提交前使用 `./mvnw -pl shopping.international-trigger -am test` 进行回归验证。

---

## 6. Safety / Do & Don’ts（禁止事项）

### 6.1 DO（应该做）

* **Do** 遵守 DDD 分层与依赖方向：

    * 新增业务逻辑时优先考虑应该放在 `domain` 聚合 / 领域服务中，而不是 Controller 里。
    * 新增的领域聚合根，实体类，值对象要是符合DDD规则的充血模型，避免逻辑满天飞
* **Do** 使用 Adapter 模式：

    * 访问数据库 / Redis / 外部 HTTP 时，先在 `domain.adapter.adapter` 定义接口，再在 `infrastructure` 实现。
* **Do** 增加或更新测试：

    * 修改领域逻辑、安全配置、变更类接口时，一定要同时维护对应测试。
* **Do** 复用已有值对象与工具类：

    * 如 `PhoneNumber`，统一校验逻辑；不要在每个 Controller 中自己写正则。
* **Do** 保持统一的 `Result` 返回格式：

    * Controller 应返回统一响应结构，错误信息集中处理。
* **Do** 编写合理的注释
    * 编写的所有类，属性和方法都要有完整的 JavaDoc，类的JavaDoc应该清楚描述本类承担了什么角色（职责），属性的JavaDoc应该描述本属性的作用（或可能的取值对于枚举），方法的JavaDoc应该清楚描述这个方法做了什么，接受什么参数，返回什么参数，可能抛出什么错误（可以灵活使用如@param，@return，@throws等标签）。：
    * 编写JavaDoc时应该使用中文 + 英文标点符号，句末不要使用句号，在遇到括号时，左括号前和右括号后应该加上空格，逗号后应该加上空格，灵活使用@code，@link等标签，以及html标签如<b>等，使得JavaDoc更加易读。

### 6.2 DON’Ts（禁止做）

1. **架构与分层相关**

* **Don’t** 让 `domain` 依赖 `trigger` / `infrastructure` / `app` 模块。
* **Don’t** 在 `trigger` 层直接注入 `Mapper` / `Repository` 实现或 Redis 客户端：

    * 这些访问应放在 `app` 或 `domain` + `infrastructure` 中。
* **Don’t** 在聚合外部直接修改聚合内部集合或状态：

    * 禁止 `user.getAddresses().add(...)`，应使用 `user.addAddress(...)` 等聚合方法。

2. **数据库 / 配置相关**

* **Don’t** 删除关键领域对象表字段：

    * 如用户 / 订单 / 地址表中的核心字段（id、user_id、status 等），除非有明确说明且配套迁移脚本。
* **Don’t** 修改生产环境配置文件：

    * 例如 `docker-compose-prod.yaml`、生产数据库连接配置。
    * 如需调整，只在 `*-dev.yaml` / `application-dev.yml` 等开发环境配置中操作。
* **Don’t** 在代码库中写死账号、密码、API Key 等敏感信息：

    * 请使用环境变量或外部化配置（`application-*.yml` 中占位符）。

3. **安全相关**

* **Don’t** 关闭或绕过安全机制：

    * 不要随意禁用 CSRF 校验。
    * 不要将所有接口放行（如对 `/api/**` 配置 `permitAll()`）。
* **Don’t** 在日志 / 响应中输出敏感数据：

    * JWT / OAuth2 Token / 验证码 / 密码摘要 等。
* **Don’t** 通过请求参数直接传递 userId 来鉴权：

    * 必须从 `SecurityContext` 中解析当前登录用户 ID。

4. **代码质量相关**

* **Don’t** 在业务代码中使用 `System.out.println`、`e.printStackTrace()`。
* **Don’t** 在 Controller 里写复杂业务逻辑：

    * Controller 只做参数绑定、鉴权、调用应用服务 / 领域服务。
* **Don’t** 将异常全部吞掉并返回“成功”：

    * 捕获异常时应记录错误日志，并返回合适的错误结构。

---
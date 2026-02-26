-- =========================================================
-- 基础：库与 SQL Mode
-- =========================================================
CREATE DATABASE IF NOT EXISTS shopdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE shopdb;

-- =========================================================
-- 1. 用户领域 (user)
-- =========================================================

-- 1.1 账户主表：本系统用户（JWT 认证）
/*
uk_user_username/email/phone：登录唯一性，避免重复注册，用于登录/找回帐号的等值查询
idx_user_status：后台用户列表按状态筛选
idx_user_last_login：近期登录活跃度排序/检索
*/
CREATE TABLE user_account
(
    id                    BIGINT UNSIGNED            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username              VARCHAR(64)                NOT NULL COMMENT '用户名(登录名)',
    nickname              VARCHAR(64)                NOT NULL COMMENT '昵称/显示名',
    email                 VARCHAR(255)               NULL COMMENT '邮箱(可空)',
    phone_country_code    VARCHAR(3)                 NULL COMMENT '手机号国家码(E.164, 不含+)',
    phone_national_number VARCHAR(14)                NULL COMMENT '手机号(E.164, 国家码之后的national number, 仅数字)',
    phone_e164            VARCHAR(16) GENERATED ALWAYS AS (
        IF(phone_country_code IS NULL, NULL, CONCAT('+', phone_country_code, phone_national_number))
        ) STORED COMMENT '手机号(E.164, 由phone_country_code+phone_national_number生成)',
    status                ENUM ('ACTIVE','DISABLED') NOT NULL DEFAULT 'DISABLED' COMMENT '账户状态',
    last_login_at         DATETIME(3)                NULL COMMENT '最近登录时间',
    is_deleted            TINYINT(1)                 NOT NULL DEFAULT 0 COMMENT '软删除标记(0否1是)',
    created_at            DATETIME(3)                NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at            DATETIME(3)                NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_phone_cc_nn (phone_country_code, phone_national_number),
    UNIQUE KEY uk_user_phone_e164 (phone_e164),
    KEY idx_user_status (status),
    KEY idx_user_last_login (last_login_at),
    CONSTRAINT chk_user_phone_pair CHECK (
        (phone_country_code IS NULL AND phone_national_number IS NULL)
            OR (phone_country_code IS NOT NULL AND phone_national_number IS NOT NULL)
        ),
    CONSTRAINT chk_user_phone_country_code CHECK (
        phone_country_code IS NULL OR phone_country_code REGEXP '^[1-9][0-9]{0,2}$'
        ),
    CONSTRAINT chk_user_phone_national_number CHECK (
        phone_national_number IS NULL OR phone_national_number REGEXP '^[0-9]{1,14}$'
        ),
    CONSTRAINT chk_user_phone_e164_len CHECK (
        phone_country_code IS NULL OR (CHAR_LENGTH(phone_country_code) + CHAR_LENGTH(phone_national_number)) <= 15
        )
) ENGINE = InnoDB COMMENT ='用户账户(本系统登录/JWT)';

-- 1.2 第三方/本地认证映射：兼容 OAuth2 登录
/*
uk_auth_provider_uid：同一 Provider 下用户唯一（OAuth2 绑定与登录命中）
idx_auth_user：按用户查看其所有绑定通道
idx_auth_provider：后台筛选某通道用户
 */
CREATE TABLE user_auth
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID, 指向 user_account.id',
    provider      ENUM ('LOCAL','GOOGLE', 'TIKTOK', 'X')
                                  NOT NULL COMMENT '认证提供方',
    issuer        VARCHAR(255)    NULL COMMENT '发行方(如 OIDC iss: https://accounts.google.com)',
    provider_uid  VARCHAR(191)    NOT NULL COMMENT '发行方内用户唯一ID(如 OIDC sub / openid)',
    password_hash VARCHAR(255)    NULL COMMENT '本地账号密码哈希(仅provider=LOCAL需要)',
    access_token  VARBINARY(1024) NULL COMMENT '访问令牌(密文/加密保存)',
    refresh_token VARBINARY(1024) NULL COMMENT '刷新令牌(密文/加密保存)',
    expires_at    DATETIME(3)     NULL COMMENT '访问令牌过期时间',
    scope         VARCHAR(512)    NULL COMMENT '授权范围',
    role          VARCHAR(64)     NULL COMMENT '角色',
    last_login_at DATETIME(3)     NULL COMMENT '该通道最近登录时间',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_provider_uid (issuer, provider_uid),
    KEY idx_auth_user (user_id),
    KEY idx_auth_provider (provider)
) ENGINE = InnoDB COMMENT ='用户认证映射(本地/OAuth2)';

-- 1.3 用户资料（扩展信息，1:1）
CREATE TABLE user_profile
(
    user_id      BIGINT UNSIGNED                  NOT NULL COMMENT '用户ID, 指向 user_account.id',
    display_name VARCHAR(64)                      NULL COMMENT '昵称/显示名',
    avatar_url   VARCHAR(500)                     NULL COMMENT '头像URL',
    gender       ENUM ('UNKNOWN','MALE','FEMALE') NOT NULL DEFAULT 'UNKNOWN' COMMENT '性别',
    birthday     DATE                             NULL COMMENT '生日',
    country      VARCHAR(64)                      NULL COMMENT '国家',
    province     VARCHAR(64)                      NULL COMMENT '省/州',
    city         VARCHAR(64)                      NULL COMMENT '城市',
    address_line VARCHAR(255)                     NULL COMMENT '地址(简单场景)',
    zipcode      VARCHAR(20)                      NULL COMMENT '邮编',
    extra        JSON                             NULL COMMENT '扩展信息(JSON)',
    created_at   DATETIME(3)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (user_id)
) ENGINE = InnoDB COMMENT ='用户资料(扩展)';

-- 1.4 用户收货地址（1:N）
/*
idx_addr_user：用户地址列表
idx_addr_user_default (user_id, is_default)：快速命中默认地址
 */
CREATE TABLE user_address
(
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id               BIGINT UNSIGNED NOT NULL COMMENT '用户ID, 指向 user_account.id',
    receiver_name         VARCHAR(64)     NOT NULL COMMENT '收货人',
    phone_country_code    VARCHAR(3)      NOT NULL COMMENT '联系电话国家码(E.164, 不含+)',
    phone_national_number VARCHAR(14)     NOT NULL COMMENT '联系电话(E.164, 国家码之后的national number, 仅数字)',
    phone_e164            VARCHAR(16) GENERATED ALWAYS AS (
        CONCAT('+', phone_country_code, phone_national_number)
        ) STORED COMMENT '联系电话(E.164, 由phone_country_code+phone_national_number生成)',
    country               VARCHAR(64)     NULL COMMENT '国家',
    province              VARCHAR(64)     NULL COMMENT '省/州',
    city                  VARCHAR(64)     NULL COMMENT '城市',
    district              VARCHAR(64)     NULL COMMENT '区/县',
    address_line1         VARCHAR(255)    NOT NULL COMMENT '地址行1',
    address_line2         VARCHAR(255)    NULL COMMENT '地址行2',
    zipcode               VARCHAR(20)     NULL COMMENT '邮编',
    is_default            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    created_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_addr_user (user_id),
    KEY idx_addr_user_default (user_id, is_default),
    KEY idx_addr_phone_e164 (phone_e164),
    CONSTRAINT chk_addr_phone_country_code CHECK (phone_country_code REGEXP '^[1-9][0-9]{0,2}$'),
    CONSTRAINT chk_addr_phone_national_number CHECK (phone_national_number REGEXP '^[0-9]{1,14}$'),
    CONSTRAINT chk_addr_phone_e164_len CHECK ((CHAR_LENGTH(phone_country_code) + CHAR_LENGTH(phone_national_number)) <=
                                              15)
) ENGINE = InnoDB COMMENT ='用户收货地址';

-- =========================================================
-- 2. 商品领域 (product)
-- =========================================================

-- 2.1 商品分类（树形）
/*
uk_cat_slug：SEO/路由稳定唯一
uk_cat_parent_name：同父节点下分类名唯一，避免重复类目
idx_cat_parent：按父ID加载子类目
idx_cat_status：后台启停筛选
idx_cat_path_prefix：按路径前缀检索
 */
CREATE TABLE product_category
(
    id         BIGINT UNSIGNED             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_id  BIGINT UNSIGNED             NULL COMMENT '父分类ID(根为空)',
    name       VARCHAR(64)                 NOT NULL COMMENT '分类名',
    slug       VARCHAR(64)                 NOT NULL COMMENT '分类别名(SEO/路由)',
    level      TINYINT                     NOT NULL DEFAULT 1 COMMENT '层级(根=1)',
    path       VARCHAR(255)                NULL COMMENT '祖先路径 如 /1/3/5/',
    sort_order INT                         NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
    status     ENUM ('ENABLED','DISABLED') NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态',
    created_at DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cat_slug (slug),
    UNIQUE KEY uk_cat_parent_name (parent_id, name),
    KEY idx_cat_parent (parent_id),
    KEY idx_cat_status (status),
    KEY idx_cat_path_prefix (path(191))
) ENGINE = InnoDB COMMENT ='商品分类(树)';

-- 2.2 商品分类 i18n（分类名/slug/品牌文案多语言）
/*
uk_cat_i18n(category_id, locale)：每分类的每语言仅 1 条覆盖记录。
uk_cat_slug_loc(locale, slug)：同一语言下 slug 唯一，保障多语言路由不冲突。
idx_cat_i18n_loc(locale)：按语言加载整棵类目/SEO 生成。
*/
CREATE TABLE product_category_i18n
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '分类ID, 指向 product_category.id',
    locale      VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    name        VARCHAR(64)     NOT NULL COMMENT '分类名(本地化)',
    slug        VARCHAR(64)     NOT NULL COMMENT '分类slug(本地化, 用于多语言路由/SEO)',
    brand       VARCHAR(120)    NULL COMMENT '品牌文案(本地化, 按你要求放于分类i18n)',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cat_i18n (category_id, locale),
    UNIQUE KEY uk_cat_slug_loc (locale, slug),
    KEY idx_cat_i18n_loc (locale)
) ENGINE = InnoDB COMMENT ='商品分类多语言覆盖';

-- 2.3 商品SPU（标准产品单元）
/*
uk_prod_slug：SEO/路由唯一
idx_prod_cat：类目下商品列表
idx_prod_status_updated (status, updated_at)：上/下架商品列表，按更新时间排序
idx_prod_default_sku：根据默认 SKU 查 SPU
ftx_prod_text (title, subtitle)：站内搜索（标题/副标题全文检索）
 */
CREATE TABLE product
(
    id              BIGINT UNSIGNED                                NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title           VARCHAR(255)                                   NOT NULL COMMENT '商品标题',
    subtitle        VARCHAR(255)                                   NULL COMMENT '副标题',
    description     TEXT                                           NULL COMMENT '商品描述',
    slug            VARCHAR(120)                                   NOT NULL COMMENT '商品别名(SEO/路由)',
    category_id     BIGINT UNSIGNED                                NOT NULL COMMENT '所属分类ID, 指向 product_category.id',
    brand           VARCHAR(120)                                   NULL COMMENT '品牌',
    cover_image_url VARCHAR(500)                                   NULL COMMENT '主图URL',
    stock_total     INT                                            NOT NULL DEFAULT 0 COMMENT '总库存(聚合)',
    sale_count      INT                                            NOT NULL DEFAULT 0 COMMENT '销量(聚合)',
    sku_type        ENUM ('SINGLE','VARIANT')                      NOT NULL DEFAULT 'SINGLE' COMMENT '规格类型(单/多规格)',
    status          ENUM ('DRAFT','ON_SALE','OFF_SHELF','DELETED') NOT NULL DEFAULT 'DRAFT' COMMENT '商品状态',
    default_sku_id  BIGINT UNSIGNED                                NULL COMMENT '默认展示SKU, 指向 product_sku.id(单规格指向唯一SKU, 多规格用于默认选中)',
    tags            JSON                                           NULL COMMENT '标签(JSON)',
    created_at      DATETIME(3)                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_prod_slug (slug),
    KEY idx_prod_cat (category_id),
    KEY idx_prod_status_updated (status, updated_at),
    KEY idx_prod_default_sku (default_sku_id),
    FULLTEXT KEY ftx_prod_text (title, subtitle)
) ENGINE = InnoDB COMMENT ='商品SPU';

-- 2.4 商品 SPU i18n（标题/副标题/描述/slug 等多语言）
/*
uk_prod_i18n(product_id, locale)：每商品每语言一条覆盖记录。
uk_prod_slug_loc(locale, slug)：同语言下 slug 唯一，保障多语言商品路由。
idx_prod_i18n_loc(locale)：按语言批量加载/导出。
ftx_prod_i18n_text：在指定语言内做站内全文检索（title/subtitle）。
*/
CREATE TABLE product_i18n
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id  BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    locale      VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    title       VARCHAR(255)    NOT NULL COMMENT '标题(本地化)',
    subtitle    VARCHAR(255)    NULL COMMENT '副标题(本地化)',
    description TEXT            NULL COMMENT '描述(本地化)',
    slug        VARCHAR(120)    NOT NULL COMMENT '商品slug(本地化, 用于多语言路由/SEO)',
    tags        JSON            NULL COMMENT '标签(本地化, 可用于站内搜索/推荐)',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_prod_i18n (product_id, locale),
    UNIQUE KEY uk_prod_slug_loc (locale, slug),
    KEY idx_prod_i18n_loc (locale),
    FULLTEXT KEY ftx_prod_i18n_text (title, subtitle)
) ENGINE = InnoDB COMMENT ='商品SPU多语言覆盖';

-- 2.5 商品SKU（销售单元/规格）
/*
uk_sku_code：对接外部/条码唯一
idx_sku_prod：SPU 下加载所有SKU
idx_sku_status：启停筛选
idx_sku_default (product_id, is_default)：根据 SPU 查默认的 SKU
 */
CREATE TABLE product_sku
(
    id         BIGINT UNSIGNED             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT UNSIGNED             NOT NULL COMMENT 'SPU ID, 指向 product.id',
    sku_code   VARCHAR(64)                 NULL COMMENT 'SKU编码(外部/条码等)',
    stock      INT                         NOT NULL DEFAULT 0 COMMENT '可售库存',
    weight     DECIMAL(10, 3)              NULL COMMENT '重量(kg)',
    status     ENUM ('ENABLED','DISABLED') NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    is_default TINYINT(1)                  NOT NULL DEFAULT 0 COMMENT '是否默认展示SKU(列表/详情默认选中)',
    barcode    VARCHAR(64)                 NULL COMMENT '条码(可空)',
    created_at DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_sku_prod (product_id),
    KEY idx_sku_status (status),
    KEY idx_sku_default (product_id, is_default)
) ENGINE = InnoDB COMMENT ='商品SKU(销售规格)';

-- 2.6 SPU 图片（多图）
/*
idx_img_prod：加载商品图集
idx_img_main (product_id, is_main)：快速命中主图
 */
CREATE TABLE product_image
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    url        VARCHAR(500)    NOT NULL COMMENT '图片URL',
    is_main    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否主图',
    sort_order INT             NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_img_prod (product_id),
    KEY idx_img_main (product_id, is_main)
) ENGINE = InnoDB COMMENT ='商品图片';

-- 2.7 SKU 图片（变体专属图, 色板图等）
/*
idx_sku_img：加载商品图集
idx_sku_img_main (product_id, is_main)：快速命中主图
 */
CREATE TABLE product_sku_image
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sku_id     BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    url        VARCHAR(500)    NOT NULL COMMENT '图片URL',
    is_main    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否主图(该SKU范围内)',
    sort_order INT             NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_sku_img (sku_id),
    KEY idx_sku_img_main (sku_id, is_main)
) ENGINE = InnoDB COMMENT ='商品图片(SKU)';

-- 2.8 商品 Like (用户-商品 映射)
/*
PK (user_id, product_id)：天然唯一，确保一人只 Like 同一商品一次
idx_like_product：统计商品被点赞人数 / 列表
 */
CREATE TABLE product_like
(
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户ID, 指向 user_account.id',
    product_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Like 时间',
    PRIMARY KEY (user_id, product_id),
    KEY idx_like_product (product_id)
) ENGINE = InnoDB COMMENT ='商品 Like 关系';

-- 2.9 SKU 多币种价格
/*
uk_price_sku_ccy：确保一个 sku 在一个结算货币中只对应一个售价
idx_price_ccy：按币种统计/导出
 */
CREATE TABLE product_price
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sku_id       BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    currency     CHAR(3)         NOT NULL COMMENT '币种, 指向 currency.code',

    -- 金额：最小货币单位（USD=cent, JPY=yen, KWD=1/1000 dinar）
    list_price   BIGINT UNSIGNED NOT NULL COMMENT '标价 (最小货币单位)',
    sale_price   BIGINT UNSIGNED NULL COMMENT '促销价 (最小货币单位, 可空)',

    is_active    TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否可售用价(预留/开关)',
    price_source ENUM ('MANUAL','FX_AUTO')
                                 NOT NULL DEFAULT 'MANUAL' COMMENT '价格来源: 手动/汇率派生',
    derived_from CHAR(3)         NULL     DEFAULT 'USD' COMMENT '派生基准币种 (通常USD), price_source=FX_AUTO时有效',

    -- FX 派生元数据
    fx_rate      DECIMAL(36, 18) NULL COMMENT '派生使用的汇率(1 derived_from = fx_rate currency)',
    fx_as_of     DATETIME(3)     NULL COMMENT '派生使用的汇率时间点',
    fx_provider  VARCHAR(32)     NULL COMMENT '派生使用的数据源',
    computed_at  DATETIME(3)     NULL COMMENT '价格计算时间',
    algo_ver     INT             NOT NULL DEFAULT 1 COMMENT '算法版本(便于未来规则升级)',
    markup_bps   INT             NOT NULL DEFAULT 0 COMMENT '加价/手续费(基点, 1%=100bps, 可为0)',

    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_price_sku_ccy (sku_id, currency),
    KEY idx_price_ccy (currency),
    KEY idx_price_ccy_active_sku (currency, is_active, sku_id),

    -- 约束：促销价 <= 标价
    CHECK (sale_price IS NULL OR sale_price <= list_price),
    CHECK (list_price > 0),
    CHECK (sale_price IS NULL OR sale_price > 0),
    -- FX_AUTO 时必须具备必要元数据（开发期建议加严，线上可视情况放松）
    CHECK (price_source <> 'FX_AUTO' OR (fx_rate IS NOT NULL AND fx_as_of IS NOT NULL AND fx_provider IS NOT NULL))
) ENGINE = InnoDB COMMENT ='SKU 多币种定价(最小货币单位 + 可审计派生元数据)';

-- 2.10 SPU 规格类别
/*
uk_spec_prod_code / uk_spec_prod_name：同一SPU下规格类别名唯一，防重复
idx_spec_prod：SPU 下加载所有规格类别
 */
CREATE TABLE product_spec
(
    id          BIGINT UNSIGNED             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id  BIGINT UNSIGNED             NOT NULL COMMENT 'SPU ID, 指向 product.id',
    spec_code   VARCHAR(64)                 NOT NULL COMMENT '类别编码(稳定): color / capacity',
    spec_name   VARCHAR(64)                 NOT NULL COMMENT '类别名称: 颜色 / 容量',
    spec_type   ENUM ('COLOR','SIZE','CAPACITY','MATERIAL','OTHER')
                                            NOT NULL DEFAULT 'OTHER' COMMENT '类别类型(用于UI渲染/业务规则)',
    is_required TINYINT(1)                  NOT NULL DEFAULT 1 COMMENT '是否必选(每个SKU必须选择一个值)',
    sort_order  INT                         NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
    status      ENUM ('ENABLED','DISABLED') NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态',
    created_at  DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at  DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_spec_prod_code (product_id, spec_code),
    UNIQUE KEY uk_spec_prod_name (product_id, spec_name),
    KEY idx_spec_prod (product_id)
) ENGINE = InnoDB COMMENT ='SPU规格类别(款式类别)';

-- 2.11 规格类别 i18n（spec 名称多语言）
/*
PK(spec_id, locale)：每规格类别在每语言唯一。
idx_spec_i18n_loc：按语言拉取面板/导出。
*/
CREATE TABLE product_spec_i18n
(
    spec_id    BIGINT UNSIGNED NOT NULL COMMENT '规格类别ID, 指向 product_spec.id',
    locale     VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    spec_name  VARCHAR(64)     NOT NULL COMMENT '规格类别名(本地化)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (spec_id, locale),
    KEY idx_spec_i18n_loc (locale)
) ENGINE = InnoDB COMMENT ='规格类别多语言';

-- 2.12 SPU 规格值
/*
uk_specval_code / uk_specval_name：同一规格类别下值名唯一，防重复
idx_specval_spec：规格类别下加载所有值
idx_specval_prod：SPU 下加载所有规格值
 */
CREATE TABLE product_spec_value
(
    id         BIGINT UNSIGNED             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT UNSIGNED             NOT NULL COMMENT 'SPU ID, 指向 product.id(冗余, 便于校验与查询)',
    spec_id    BIGINT UNSIGNED             NOT NULL COMMENT '规格类别ID, 指向 product_spec.id',
    value_code VARCHAR(64)                 NOT NULL COMMENT '值编码(稳定): black / gray / 512gb',
    value_name VARCHAR(64)                 NOT NULL COMMENT '值名称: 黑色 / 灰色 / 512GB',
    attributes JSON                        NULL COMMENT '附加属性: 如颜色hex、展示图等',
    sort_order INT                         NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
    status     ENUM ('ENABLED','DISABLED') NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态',
    created_at DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_specval_code (spec_id, value_code),
    UNIQUE KEY uk_specval_name (spec_id, value_name),
    KEY idx_specval_spec (spec_id),
    KEY idx_specval_prod (product_id)
) ENGINE = InnoDB COMMENT ='规格值(款式值)';

-- 2.13 规格值 i18n（value 名称多语言）
/*
PK(value_id, locale)：每规格值在每语言唯一。
idx_specval_i18n_loc：按语言拉取面板/导出。
*/
CREATE TABLE product_spec_value_i18n
(
    value_id   BIGINT UNSIGNED NOT NULL COMMENT '规格值ID, 指向 product_spec_value.id',
    locale     VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    value_name VARCHAR(64)     NOT NULL COMMENT '规格值名(本地化)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (value_id, locale),
    KEY idx_specval_i18n_loc (locale)
) ENGINE = InnoDB COMMENT ='规格值多语言';

-- 2.14 SKU-规格值映射
/*
idx_pss_value：按规格值统计/导出
idx_pss_spec_value：按规格类别-值组合统计/导出
 */
CREATE TABLE product_sku_spec
(
    sku_id     BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    spec_id    BIGINT UNSIGNED NOT NULL COMMENT '规格类别ID, 指向 product_spec.id',
    value_id   BIGINT UNSIGNED NOT NULL COMMENT '规格值ID, 指向 product_spec_value.id',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (sku_id, spec_id),
    KEY idx_pss_value (value_id),
    KEY idx_pss_spec_value (spec_id, value_id)
) ENGINE = InnoDB COMMENT ='SKU-规格值映射(每类别恰好一值)';

-- 2.15 最新汇率快照
/**
idx_fx_latest_quote：按报价币种查询
idx_fx_latest_provider_time：按提供方 + 采样时间点查询
 */
CREATE TABLE fx_rate_latest
(
    base_code  CHAR(3)         NOT NULL COMMENT '基准币种(如 USD), 指向 currency.code',
    quote_code CHAR(3)         NOT NULL COMMENT '报价币种(如 EUR), 指向 currency.code',
    rate       DECIMAL(36, 18) NOT NULL COMMENT '1 base = rate quote',
    as_of      DATETIME(3)     NOT NULL COMMENT '汇率时间点/采样时间',
    provider   VARCHAR(32)     NOT NULL COMMENT '数据源(如 ECB / OpenExchangeRates / ...)',
    updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (base_code, quote_code),
    KEY idx_fx_latest_quote (quote_code),
    KEY idx_fx_latest_provider_time (provider, as_of)
) ENGINE = InnoDB COMMENT ='最新汇率快照(用于派生价格/展示换算)';

-- 2.16 汇率历史记录
CREATE TABLE fx_rate
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    base_code  CHAR(3)         NOT NULL COMMENT '基准币种(如 USD), 指向 currency.code',
    quote_code CHAR(3)         NOT NULL COMMENT '报价币种(如 EUR), 指向 currency.code',
    rate       DECIMAL(36, 18) NOT NULL COMMENT '1 base = rate quote',
    as_of      DATETIME(3)     NOT NULL COMMENT '汇率时间点/采样时间',
    provider   VARCHAR(32)     NOT NULL COMMENT '数据源(如 ECB / OpenExchangeRates)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '写入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fx_pair_provider_time (base_code, quote_code, provider, as_of),
    KEY idx_fx_pair_time (base_code, quote_code, as_of),
    KEY idx_fx_provider_time (provider, as_of)
) ENGINE = InnoDB COMMENT ='汇率历史(审计/回放/对账解释用)';

-- =========================================================
-- 3. 订单领域 (orders)
-- =========================================================

-- 3.1
/*
uk_order_no：对外单号唯一（便于客服/对账）
idx_order_user：用户订单列表
idx_order_status_time (status, created_at)：状态页/后台列表（按创建时间排序）
idx_order_created：时间区间检索
idx_order_payment_ext：由 externalId 反查订单（回调/轮询场景）
 */
CREATE TABLE orders
(
    id                     BIGINT UNSIGNED                                                      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no               CHAR(26)                                                             NOT NULL COMMENT '业务单号(如ULID/雪花, 对外展示)',
    user_id                BIGINT UNSIGNED                                                      NOT NULL COMMENT '下单用户ID, 指向 user_account.id',
    status                 ENUM ('CREATED','PENDING_PAYMENT','PAID','CANCELLED','CLOSED','FULFILLED','REFUNDING','REFUNDED')
                                                                                                NOT NULL DEFAULT 'CREATED' COMMENT '订单状态机',
    items_count            INT                                                                  NOT NULL DEFAULT 0 COMMENT '商品总件数',

    total_amount           BIGINT UNSIGNED                                                      NOT NULL COMMENT '商品总额(未税/未含运费/未含折扣, 最小货币单位)',
    discount_amount        BIGINT UNSIGNED                                                      NOT NULL DEFAULT 0 COMMENT '折扣总额(最小货币单位)',
    shipping_amount        BIGINT UNSIGNED                                                      NOT NULL DEFAULT 0 COMMENT '运费(最小货币单位)',
    tax_amount             BIGINT UNSIGNED                                                      NOT NULL DEFAULT 0 COMMENT '税费(结算计算, 最小货币单位)',
    pay_amount             BIGINT UNSIGNED                                                      NOT NULL COMMENT '应付金额=总额-折扣+运费+税费(最小货币单位)',

    currency               CHAR(3)                                                              NOT NULL DEFAULT 'USD' COMMENT '币种, 指向 currency.code',
    pay_channel            ENUM ('NONE','ALIPAY','WECHAT','STRIPE','PAYPAL','OTHER')            NOT NULL DEFAULT 'NONE' COMMENT '支付通道',
    pay_status             ENUM ('NONE','INIT','PENDING','SUCCESS','FAIL','CLOSED','EXCEPTION') NOT NULL DEFAULT 'NONE' COMMENT '支付状态(网关侧)',
    payment_external_id    VARCHAR(128)                                                         NULL COMMENT '支付单 externalId(网关唯一标记)',
    active_payment_id      BIGINT UNSIGNED                                                      NULL COMMENT '当前有效支付单ID, 指向 payment_order.id',
    pay_time               DATETIME(3)                                                          NULL COMMENT '支付成功时间',
    address_snapshot       JSON                                                                 NULL COMMENT '收货信息快照(JSON)',
    address_changed        TINYINT(1)                                                           NOT NULL DEFAULT 0 COMMENT '是否已修改过地址(仅一次)',
    buyer_remark           VARCHAR(500)                                                         NULL COMMENT '买家留言',
    idempotency_key        VARCHAR(64)                                                          NULL COMMENT '幂等键(防重复下单)',
    cancel_reason          VARCHAR(255)                                                         NULL COMMENT '取消原因',
    cancel_time            DATETIME(3)                                                          NULL COMMENT '取消时间',
    refund_reason_snapshot JSON                                                                 NULL COMMENT '退款原因快照(JSON)',
    created_at             DATETIME(3)                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at             DATETIME(3)                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_order_idempotency (user_id, idempotency_key),
    KEY idx_order_user (user_id),
    KEY idx_order_status_time (status, created_at),
    KEY idx_order_created (created_at),
    KEY idx_order_payment_ext (payment_external_id),
    KEY idx_order_active_payment (active_payment_id),

    CHECK (total_amount > 0),
    CHECK (discount_amount >= 0),
    CHECK (shipping_amount >= 0),
    CHECK (tax_amount >= 0),
    CHECK (pay_amount > 0)
) ENGINE = InnoDB COMMENT ='订单主表(多币种/最小货币单位/含税费字段)';

-- 3.2 订单明细
/*
idx_item_order：加载订单明细
idx_item_prod / idx_item_sku：基于商品/SKU 的售卖分析
idx_item_discount_code：从折扣码侧统计使用情况/回溯明细。
 */
CREATE TABLE order_item
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id         BIGINT UNSIGNED NOT NULL COMMENT '订单 ID, 指向 orders.id',
    product_id       BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    sku_id           BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    discount_code_id BIGINT UNSIGNED NULL COMMENT '折扣码ID, 指向 discount_code.id',
    title            VARCHAR(255)    NOT NULL COMMENT '商品标题快照',
    sku_attrs        JSON            NULL COMMENT 'SKU属性快照(JSON)',
    cover_image_url  VARCHAR(500)    NULL COMMENT '商品图快照',
    unit_price       BIGINT UNSIGNED NOT NULL COMMENT '单价快照',
    quantity         INT             NOT NULL COMMENT '数量',
    subtotal_amount  BIGINT UNSIGNED NOT NULL COMMENT '小计=单价*数量',
    created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_item_order (order_id),
    KEY idx_item_prod (product_id),
    KEY idx_item_sku (sku_id),
    KEY idx_item_discount_code (discount_code_id)
) ENGINE = InnoDB COMMENT ='订单明细';

-- 3.3 订单状态流转日志（状态机审计）
/*
idx_osl_order：订单状态流转历史查询
 */
CREATE TABLE order_status_log
(
    id           BIGINT UNSIGNED                                                                   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id     BIGINT UNSIGNED                                                                   NOT NULL COMMENT '订单ID, 指向 orders.id',
    event_source ENUM ('SYSTEM','USER','PAYMENT_CALLBACK','SCHEDULER','ADMIN','SHIPPING_CALLBACK') NOT NULL DEFAULT 'SYSTEM' COMMENT '事件来源',
    from_status  VARCHAR(32)                                                                       NULL COMMENT '源状态',
    to_status    VARCHAR(32)                                                                       NOT NULL COMMENT '目标状态',
    note         VARCHAR(255)                                                                      NULL COMMENT '备注',
    created_at   DATETIME(3)                                                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_osl_order (order_id)
) ENGINE = InnoDB COMMENT ='订单状态流转日志';

-- 3.4 库存日志（预占/扣减/释放）
/*
idx_inv_sku_time (sku_id, created_at)：按SKU时间序查询流水
idx_inv_order：从订单侧定位库存流水
 */
CREATE TABLE inventory_log
(
    id          BIGINT UNSIGNED                               NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sku_id      BIGINT UNSIGNED                               NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    order_id    BIGINT UNSIGNED                               NOT NULL COMMENT '关联订单ID',
    change_type ENUM ('RESERVE','DEDUCT','RELEASE','RESTOCK') NOT NULL COMMENT '变更类型:预占/扣减/释放/入库',
    quantity    INT                                           NOT NULL COMMENT '变更数量(正数)',
    reason      VARCHAR(255)                                  NULL COMMENT '原因备注',
    created_at  DATETIME(3)                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_order_sku_type (order_id, sku_id, change_type),
    KEY idx_inv_sku_time (sku_id, created_at),
    KEY idx_inv_order (order_id)
) ENGINE = InnoDB COMMENT ='库存变动日志';

-- 3.5 购物车(用户-SKU 映射)
/*
uk_cart_user_sku (user_id, sku_id)：去重一人一SKU一条
idx_cart_user：用户购物车列表
 */
CREATE TABLE shopping_cart_item
(
    id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id  BIGINT UNSIGNED NOT NULL COMMENT '用户ID, 指向 user_account.id',
    sku_id   BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    quantity INT             NOT NULL DEFAULT 1 COMMENT '数量',
    selected TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否勾选',
    added_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_sku (user_id, sku_id),
    KEY idx_cart_user (user_id)
) ENGINE = InnoDB COMMENT ='购物车条目';

-- 3.6 折扣策略（策略模板，供折扣码引用）
/*
idx_policy_scope_type：后台按“作用域+类型”筛选策略。
CHECK：基础约束（百分比范围/金额为正；不同类型下字段取值限制）。
*/
CREATE TABLE discount_policy
(
    id            BIGINT UNSIGNED           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name          VARCHAR(120)              NOT NULL COMMENT '策略名称',
    apply_scope   ENUM ('ORDER','ITEM')     NOT NULL DEFAULT 'ITEM' COMMENT '作用域：整单/单行',
    strategy_type ENUM ('PERCENT','AMOUNT') NOT NULL COMMENT '折扣类型：按百分比 or 按固定金额',
    percent_off   DECIMAL(5, 2)             NULL COMMENT '折扣百分比(0~100), 当 strategy_type=PERCENT 时生效',
    created_at    DATETIME(3)               NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)               NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_policy_scope_type (apply_scope, strategy_type),
    -- 约束：百分比策略必须有 percent_off, 金额策略必须没有 percent_off
    CHECK (strategy_type <> 'PERCENT' OR (percent_off IS NOT NULL AND percent_off > 0 AND percent_off < 100)),
    CHECK (strategy_type <> 'AMOUNT' OR percent_off IS NULL)
) ENGINE = InnoDB COMMENT ='折扣策略模板(骨架,金额配置下沉到 discount_policy_amount)';

-- 3.7 折扣策略 - 币种金额配置（支持不同币种不同面额/门槛/封顶）
/*
 对 strategy_type=AMOUNT：amount_off 必填（业务层控制）
 */
CREATE TABLE discount_policy_amount
(
    policy_id           BIGINT UNSIGNED NOT NULL COMMENT '折扣策略ID, 指向 discount_policy.id',
    currency            CHAR(3)         NOT NULL COMMENT '币种, 指向 currency.code',

    amount_off          BIGINT UNSIGNED NULL COMMENT '固定减金额(最小货币单位)',
    min_order_amount    BIGINT UNSIGNED NULL COMMENT '门槛金额(最小货币单位)',
    max_discount_amount BIGINT UNSIGNED NULL COMMENT '封顶减免金额(最小货币单位)',

    amount_source       ENUM ('MANUAL','FX_AUTO')
                                        NOT NULL DEFAULT 'MANUAL' COMMENT '金额来源: 手动/汇率派生(未显式传入币种时自动派生)',
    derived_from        CHAR(3)         NULL     DEFAULT 'USD' COMMENT '派生基准币种 (通常USD), amount_source=FX_AUTO时有效',

    -- FX 派生元数据(用于审计与更新时“仅更新自动派生项”)
    fx_rate             DECIMAL(36, 18) NULL COMMENT '派生使用的汇率(1 derived_from = fx_rate currency)',
    fx_as_of            DATETIME(3)     NULL COMMENT '派生使用的汇率时间点',
    fx_provider         VARCHAR(32)     NULL COMMENT '派生使用的数据源',
    computed_at         DATETIME(3)     NULL COMMENT '金额计算时间',

    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    PRIMARY KEY (policy_id, currency),
    KEY idx_dpa_currency (currency),
    CHECK (amount_off IS NULL OR amount_off > 0),
    CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    -- FX_AUTO 时必须具备必要元数据（开发期建议加严，线上可视情况放松）
    CHECK (amount_source <> 'FX_AUTO' OR
           (derived_from IS NOT NULL AND fx_rate IS NOT NULL AND fx_as_of IS NOT NULL AND fx_provider IS NOT NULL AND
            computed_at IS NOT NULL))
) ENGINE = InnoDB COMMENT ='折扣策略-币种金额配置(AMOUNT/门槛/封顶多币种化)';

-- 3.8 折扣码（6位字母数字，唯一；关联策略；统计使用次数）
/*
uk_coupon_code：折扣码文本唯一，快速命中。
idx_coupon_policy：按策略聚合查询/运营分析。
idx_coupon_expires：过期清理/有效性筛查。
idx_coupon_scope：按可用范围模式筛选。
CHECK code：限定大写字母数字6位。
*/
CREATE TABLE discount_code
(
    id         BIGINT UNSIGNED                  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code       CHAR(6)                          NOT NULL COMMENT '折扣码(6位A-Z/0-9)',
    policy_id  BIGINT UNSIGNED                  NOT NULL COMMENT '折扣策略ID, 指向 discount_policy.id',
    name       VARCHAR(120)                     NOT NULL COMMENT '折扣码名称(运营标识)',
    scope_mode ENUM ('ALL','INCLUDE','EXCLUDE') NOT NULL DEFAULT 'INCLUDE' COMMENT '适用范围模式',
    expires_at DATETIME(3)                      NULL COMMENT '过期时间(到期不可用, permanent=1 时必须为空)',
    permanent  TINYINT(1)                       NOT NULL DEFAULT 0 COMMENT '是否永久有效(1=永久;0=需expires_at)',
    created_at DATETIME(3)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_code (code),
    KEY idx_coupon_policy (policy_id),
    KEY idx_coupon_expires (expires_at),
    KEY idx_coupon_permanent (permanent),
    KEY idx_coupon_scope (scope_mode),
    CHECK (code REGEXP '^[A-Z0-9]{6}$'),
    CHECK ((permanent = 1 AND expires_at IS NULL) OR (permanent = 0 AND expires_at IS NOT NULL))
) ENGINE = InnoDB COMMENT ='折扣码';

-- 3.9 折扣码-商品SPU映射（限制适用范围）
/*
PK (discount_code_id, product_id)：去重“一码-一SPU”。
idx_cpnprod_product：从SPU反查可用折扣码（商品页展示）。
*/
CREATE TABLE discount_code_product
(
    discount_code_id BIGINT UNSIGNED NOT NULL COMMENT '折扣码ID, 指向 discount_code.id',
    product_id       BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (discount_code_id, product_id),
    KEY idx_cpnprod_product (product_id)
) ENGINE = InnoDB COMMENT ='折扣码-商品SPU映射(限定适用范围)';

-- 3.10 折扣使用日志（作为“真实使用”与对账的事实表）
/*
idx_oda_order：按订单聚合查询（售后/对账）。
idx_oda_code：按折扣码查询使用情况。
唯一约束：
uk_oda_item_once (order_item_id, discount_code_id)：同一明细同一折扣码仅记录一次。
uk_oda_order_once (order_id, discount_code_id, order_level_only)：整单层仅记录一次（利用生成列区分 NULL）。
*/
CREATE TABLE order_discount_applied
(
    id                  BIGINT UNSIGNED       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT UNSIGNED       NOT NULL COMMENT '订单ID, 指向 orders.id',
    order_item_id       BIGINT UNSIGNED       NULL COMMENT '订单明细ID, 为空表示订单级折扣',
    discount_code_id    BIGINT UNSIGNED       NOT NULL COMMENT '折扣码ID, 指向 discount_code.id',
    applied_scope       ENUM ('ORDER','ITEM') NOT NULL COMMENT '应用范围：订单级/明细级',
    currency            CHAR(3)               NOT NULL COMMENT '币种(冗余自 orders.currency)',
    applied_amount      BIGINT UNSIGNED       NOT NULL COMMENT '本次实际抵扣金额(最小货币单位)',
    base_currency       CHAR(3)               NOT NULL DEFAULT 'USD' COMMENT '统一记账币种(全站默认)',
    applied_amount_base BIGINT UNSIGNED       NOT NULL COMMENT '本次实际抵扣金额(统一记账币种,最小货币单位)',
    fx_rate             DECIMAL(36, 18)       NULL COMMENT '折扣换算汇率快照(1 base = rate quote), base=base_currency, quote=currency',
    fx_as_of            DATETIME(3)           NULL COMMENT '汇率时间点/采样时间(快照)',
    fx_provider         VARCHAR(32)           NULL COMMENT '汇率数据源(快照)',
    created_at          DATETIME(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_oda_order (order_id),
    KEY idx_oda_code (discount_code_id),
    -- 明细级幂等：同一明细+同一码只允许一条
    UNIQUE KEY uk_oda_item_once (order_item_id, discount_code_id),
    -- 订单级幂等：同一订单+同一码只允许一条（借助生成列处理 order_item_id 为 NULL 的情况）
    order_level_only    TINYINT AS (CASE WHEN order_item_id IS NULL THEN 1 ELSE NULL END) STORED,
    UNIQUE KEY uk_oda_order_once (order_id, discount_code_id, order_level_only),
    CHECK (applied_amount > 0)
) ENGINE = InnoDB COMMENT ='折扣应用日志(真实使用的事实表)';

-- =========================================================
-- 4. 支付领域 (payment)
-- =========================================================

-- 4.1 支付单（对接支付API，使用 externalId 关联）  /* 待定, 视支付通道 API 文档而定 */
/*
uk_pay_external：支付网关 externalId 唯一（幂等与关联）
idx_pay_order：从订单查支付单
idx_pay_status_update (status, updated_at)：按状态+时间扫描（轮询重试/清理）
 */
CREATE TABLE payment_order
(
    id               BIGINT UNSIGNED                                                      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id         BIGINT UNSIGNED                                                      NOT NULL COMMENT '订单ID, 指向 orders.id',
    external_id      VARCHAR(128)                                                         NULL COMMENT '支付网关 externalId(唯一)',
    capture_id       VARCHAR(128)                                                         NULL COMMENT 'capture ID (用于退款, 目前仅 PayPal 需要)',
    channel          ENUM ('NONE','ALIPAY','WECHAT','STRIPE','PAYPAL','OTHER')            NOT NULL COMMENT '支付通道',
    amount           BIGINT UNSIGNED                                                      NOT NULL COMMENT '支付金额',
    currency         CHAR(3)                                                              NOT NULL DEFAULT 'USD' COMMENT '币种',
    status           ENUM ('NONE','INIT','PENDING','SUCCESS','FAIL','CLOSED','EXCEPTION') NOT NULL DEFAULT 'NONE' COMMENT '支付单状态',
    request_payload  JSON                                                                 NULL COMMENT '下单请求报文(JSON)',
    response_payload JSON                                                                 NULL COMMENT '下单响应报文(JSON)',
    notify_payload   JSON                                                                 NULL COMMENT '最近一次回调报文(JSON)',
    last_polled_at   DATETIME(3)                                                          NULL COMMENT '最近轮询时间',
    last_notified_at DATETIME(3)                                                          NULL COMMENT '最近回调处理时间',
    created_at       DATETIME(3)                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at       DATETIME(3)                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_external (external_id),
    KEY idx_pay_order (order_id),
    KEY idx_pay_status_update (status, updated_at)
) ENGINE = InnoDB COMMENT ='支付单(网关externalId对应)';

-- 4.2 支付退款单（对账/幂等/回调 & 轮询）
/*
uk_refund_no：内部退款单号唯一（ULID/雪花）
uk_refund_external：网关退款 external_id 唯一（对账/幂等）
uk_refund_req_dedupe：(payment_order_id, client_refund_no) 客户端/业务侧幂等键
idx_refund_order / idx_refund_payment：从订单/原支付定位退款
idx_refund_status_update：(status, updated_at) 退款轮询/看板
idx_refund_ticket：从客服工单反查退款
CHECK：金额为正
*/
CREATE TABLE payment_refund
(
    id                 BIGINT UNSIGNED                                   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    refund_no          CHAR(26)                                          NOT NULL COMMENT '退款单号(ULID/雪花等, 对外可见)',
    order_id           BIGINT UNSIGNED                                   NOT NULL COMMENT '订单ID, 指向 orders.id',
    payment_order_id   BIGINT UNSIGNED                                   NOT NULL COMMENT '原支付单ID, 指向 payment_order.id',

    external_refund_id VARCHAR(128)                                      NULL COMMENT '支付系统退款 externalId(唯一)',
    client_refund_no   VARCHAR(64)                                       NULL COMMENT '商户侧/客户端幂等键(可选)',

    amount             BIGINT UNSIGNED                                   NOT NULL COMMENT '退款总金额(订单币种)',
    currency           CHAR(3)                                           NOT NULL DEFAULT 'USD' COMMENT '币种',
    -- 可选细分(便于财务拆解；留空则仅使用 amount)
    items_amount       BIGINT UNSIGNED                                   NULL COMMENT '货品部分的退款金额',
    shipping_amount    BIGINT UNSIGNED                                   NULL COMMENT '运费部分的退款金额',

    status             ENUM ('INIT','PENDING','SUCCESS','FAIL','CLOSED') NOT NULL DEFAULT 'INIT' COMMENT '退款状态',
    reason_code        ENUM ('CUSTOMER_REQUEST','RETURNED','LOST','DAMAGED','PRICE_ADJUST','DUPLICATE','EXCEPTION','OTHER')
                                                                         NOT NULL DEFAULT 'OTHER' COMMENT '原因分类',
    reason_text        VARCHAR(255)                                      NULL COMMENT '原因备注(自由文本)',
    initiator          ENUM ('USER','ADMIN','SYSTEM','SCHEDULER')        NOT NULL DEFAULT 'ADMIN' COMMENT '发起方',
    ticket_id          BIGINT UNSIGNED                                   NULL COMMENT '关联工单, 指向 cs_ticket.id',

    request_payload    JSON                                              NULL COMMENT '退款请求报文(JSON)',
    response_payload   JSON                                              NULL COMMENT '退款响应报文(JSON)',
    notify_payload     JSON                                              NULL COMMENT '最近一次回调报文(JSON)',
    last_polled_at     DATETIME(3)                                       NULL COMMENT '最近轮询时间',
    last_notified_at   DATETIME(3)                                       NULL COMMENT '最近回调处理时间',

    created_at         DATETIME(3)                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at         DATETIME(3)                                       NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_refund_external (external_refund_id),
    UNIQUE KEY uk_refund_req_dedupe (payment_order_id, client_refund_no),
    KEY idx_refund_order (order_id),
    KEY idx_refund_payment (payment_order_id),
    KEY idx_refund_status_update (status, updated_at),
    KEY idx_refund_ticket (ticket_id),
    CHECK (amount > 0),
    CHECK (items_amount IS NULL OR items_amount >= 0),
    CHECK (shipping_amount IS NULL OR shipping_amount >= 0)
) ENGINE = InnoDB COMMENT ='支付退款单(网关 externalId 对应)';

-- 4.3 支付退款明细（可选：按订单明细拆分记录，用于部分退款对账）
/*
idx_rfditem_refund：通过退款单查明细
idx_rfditem_orderitem：通过订单明细查其被退款记录
CHECK：数量/金额为正；超退校验在业务层做
*/
CREATE TABLE payment_refund_item
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    refund_id     BIGINT UNSIGNED NOT NULL COMMENT '退款单ID, 指向 payment_refund.id',
    order_item_id BIGINT UNSIGNED NOT NULL COMMENT '订单明细ID, 指向 order_item.id',
    quantity      INT             NOT NULL COMMENT '本次退款数量(件)',
    amount        BIGINT UNSIGNED NOT NULL COMMENT '该明细对应的退款金额',
    reason        VARCHAR(255)    NULL COMMENT '该明细退款原因备注',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_rfditem_refund (refund_id),
    KEY idx_rfditem_orderitem (order_item_id),
    CHECK (quantity > 0),
    CHECK (amount > 0)
) ENGINE = InnoDB COMMENT ='退款明细(按订单明细拆分)';

-- =========================================================
-- 5. 物流领域 (shipping)
-- =========================================================

-- 5.1 物流包裹（一个包裹对应一张国际运单；支持后续末端换单）
/*
uk_carrier_tracking (carrier_code, tracking_no)：对接回调/查询时以“承运商+单号”精准命中；避免重复创建
uk_ship_external：三方下单幂等
idx_ship_order：从订单查所有包裹（合单/拆单均可）
idx_ship_status_updated：定时轮询仅扫待更新状态+时间近的记录
 */
CREATE TABLE shipment
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    shipment_no     CHAR(26)        NOT NULL COMMENT '内部包裹号(ULID/雪花等)',
    order_id        BIGINT UNSIGNED NULL COMMENT '发起来源主订单ID, 指向 orders.id (支持合单场景可为空)',
    idempotency_key VARCHAR(64)     NULL COMMENT '幂等键(防重复下单)',
    carrier_code    VARCHAR(64)     NULL COMMENT '承运商编码(标准化如 dhl, ups, 4px, yanwen)',
    carrier_name    VARCHAR(128)    NULL COMMENT '承运商名称',
    service_code    VARCHAR(64)     NULL COMMENT '服务/产品代码(如 DHL_ECOM)',
    tracking_no     VARCHAR(128)    NULL COMMENT '承运商运单号/追踪号(可能下单后才生成)',
    ext_external_id VARCHAR(128)    NULL COMMENT '对接物流API的externalId/订单号',
    /*
     CREATED: 系统内运单已创建, 未向承运商下单（未拿到面单号）
     LABEL_CREATED: 已经拿到面单号
     PICKED_UP: 已被揽收
     IN_TRANSIT: 运输中
     CUSTOMS_PROCESSING: 清关中
     CUSTOMS_HOLD: 海关暂扣
     CUSTOMS_RELEASED: 清关放行
     HANDED_OVER: 进入下一运段
     OUT_FOR_DELIVERY: 派送中
     DELIVERED: 签收完成（终态）
     EXCEPTION: 异常（可为终态或中间态，后续可能被重派，回退，或客户改地址后继续流转）
     RETURNED: 退回（终态）
     CANCELLED: 取消（面单作废，终态）
     LOST: 丢失（由承运商判定或长时间无轨迹且调度认定丢失，终态）
     */
    status          ENUM ('CREATED','LABEL_CREATED','PICKED_UP','IN_TRANSIT','CUSTOMS_PROCESSING','CUSTOMS_HOLD','CUSTOMS_RELEASED',
        'HANDED_OVER','OUT_FOR_DELIVERY','DELIVERED','EXCEPTION','RETURNED','CANCELLED','LOST')
                                    NOT NULL DEFAULT 'CREATED' COMMENT '物流状态',
    ship_from       JSON            NULL COMMENT '发货地/仓信息快照(JSON)',
    ship_to         JSON            NULL COMMENT '收件地址快照(JSON); 与订单快照可能不同',
    weight_kg       DECIMAL(10, 3)  NULL COMMENT '毛重(kg)',
    length_cm       DECIMAL(10, 1)  NULL COMMENT '长(cm)',
    width_cm        DECIMAL(10, 1)  NULL COMMENT '宽(cm)',
    height_cm       DECIMAL(10, 1)  NULL COMMENT '高(cm)',
    declared_value  BIGINT UNSIGNED NULL COMMENT '申报价值(币种见currency)',
    currency        CHAR(3)         NOT NULL DEFAULT 'USD' COMMENT '币种',
    customs_info    JSON            NULL COMMENT '关务/清关信息(HS code, 原产地, 税费等)',
    label_url       VARCHAR(500)    NULL COMMENT '电子面单URL(可选)',
    pickup_time     DATETIME(3)     NULL COMMENT '揽收时间',
    delivered_time  DATETIME(3)     NULL COMMENT '签收时间',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shipment_no (shipment_no),
    UNIQUE KEY (order_id, idempotency_key),
    UNIQUE KEY uk_carrier_tracking (carrier_code, tracking_no), -- 同一承运商下运单号唯一
    UNIQUE KEY uk_ship_external (ext_external_id),              -- 对接方externalId确保幂等
    KEY idx_ship_order (order_id),                              -- 从订单查包裹
    KEY idx_ship_status_updated (status, updated_at),           -- 任务轮询/看板扫描
    KEY idx_ship_created (created_at)                           -- 时间窗口扫描
) ENGINE = InnoDB COMMENT ='物流包裹/运单(跨境)';

-- 5.2 包裹内商品映射（支持合单/拆单：N:N）
/*
uk_ship_orderitem：避免同包裹里重复挂载同一 order_item
idx_shipitem_order：订单详情页快速反查“包含哪些包裹”
 */
CREATE TABLE shipment_item
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    shipment_id   BIGINT UNSIGNED NOT NULL COMMENT '包裹ID, 指向 shipment.id',
    order_id      BIGINT UNSIGNED NOT NULL COMMENT '订单ID, 指向 orders.id(便于跨表检索)',
    order_item_id BIGINT UNSIGNED NOT NULL COMMENT '订单明细ID, 指向 order_item.id',
    product_id    BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    sku_id        BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    quantity      INT             NOT NULL COMMENT '该包裹中该明细的数量(支持部分发货)',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ship_orderitem (shipment_id, order_item_id), -- 一个包裹中，同一明细不重复
    KEY idx_shipitem_shipment (shipment_id),                   -- 从包裹查其包含的明细
    KEY idx_shipitem_order (order_id),                         -- 从订单反查包裹(合单/拆单)
    KEY idx_shipitem_sku (sku_id)                              -- 统计SKU发货情况
) ENGINE = InnoDB COMMENT ='包裹-订单明细映射(支持合单与拆单)';

-- 5.3 包裹状态流转日志（源状态→目标状态 + 事件来源）
/*
idx_ssl_ship_time：按包裹时间线查询与“取最新状态”高频
idx_ssl_to_status：统计流入某状态的包裹量（看板/报表）
idx_ssl_source：按来源排查, 回放某时间段的处理
 */
CREATE TABLE shipment_status_log
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    shipment_id   BIGINT UNSIGNED NOT NULL COMMENT '包裹ID,指向 shipment.id',
    -- 状态机：与 shipment.status 同口径
    from_status   ENUM ('CREATED','LABEL_CREATED','PICKED_UP','IN_TRANSIT','CUSTOMS_PROCESSING','CUSTOMS_HOLD','CUSTOMS_RELEASED',
        'HANDED_OVER','OUT_FOR_DELIVERY','DELIVERED','EXCEPTION','RETURNED','CANCELLED','LOST')
                                  NULL COMMENT '变更前状态 (首个状态可为空)',
    to_status     ENUM ('CREATED','LABEL_CREATED','PICKED_UP','IN_TRANSIT','CUSTOMS_PROCESSING','CUSTOMS_HOLD','CUSTOMS_RELEASED',
        'HANDED_OVER','OUT_FOR_DELIVERY','DELIVERED','EXCEPTION','RETURNED','CANCELLED','LOST')
                                  NOT NULL COMMENT '变更后状态',
    -- 事件发生时间与来源（谁触发, 如何触发）
    event_time    DATETIME(3)     NULL COMMENT '状态发生时间 (承运商/系统口径，空则参考created_at)',
    source_type   ENUM ('CARRIER_WEBHOOK','CARRIER_POLL','SYSTEM_JOB','MANUAL','API')
                                  NOT NULL COMMENT '事件来源类型：承运商回调/轮询/系统任务/人工/开放接口',
    source_ref    VARCHAR(128)    NOT NULL COMMENT '来源引用ID (如回调notify_id, 任务run_id, 请求id)',
    carrier_code  VARCHAR(64)     NULL COMMENT '承运商编码 (来源于承运商事件时可填)',
    tracking_no   VARCHAR(128)    NULL COMMENT '当次事件涉及的追踪号 (干线或末端)',
    note          VARCHAR(255)    NULL COMMENT '备注/原因 (例如异常说明, 人工操作说明)',
    raw_payload   JSON            NULL COMMENT '原始报文 (承运商回调/接口入参等)',
    actor_user_id BIGINT UNSIGNED NULL COMMENT '操作者用户ID (MANUAL/API可记录)',

    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '写入时间',

    PRIMARY KEY (id),

    -- 幂等去重（避免同一来源, 同一引用ID对同一包裹重复落库）
    UNIQUE KEY uk_ssl_dedupe (shipment_id, source_type, source_ref),

    -- 常用查询索引
    KEY idx_ssl_ship_time (shipment_id, (COALESCE(event_time, created_at))), -- 时间序回放, 取最新节点
    KEY idx_ssl_to_status (to_status),                                       -- 看板统计“流入某状态”的数量
    KEY idx_ssl_source (source_type, created_at)                             -- 分来源按时间筛查

) ENGINE = InnoDB COMMENT ='包裹状态流转日志 (源状态→目标状态，含事件来源与原始报文)';

-- 承运商理赔单
/*
uk_claim_external：避免同一外部编号重复落库
idx_claim_shipment / idx_claim_order / idx_claim_reship / idx_claim_ticket：从包裹/订单/补寄/工单反查理赔单
 */
CREATE TABLE shipping_claim
(
    id           BIGINT UNSIGNED                                                     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    ticket_id    BIGINT UNSIGNED                                                     NULL COMMENT '工单ID, 指向 cs_ticket.id',
    shipment_id  BIGINT UNSIGNED                                                     NOT NULL COMMENT '包裹ID, 指向 shipment.id',
    order_id     BIGINT UNSIGNED                                                     NOT NULL COMMENT '订单ID, 指向 orders.id',
    reship_id    BIGINT UNSIGNED                                                     NULL COMMENT '补发单ID, 指向 aftersales_reship.id',
    carrier_code VARCHAR(64)                                                         NOT NULL COMMENT '承运商编码 (来源于承运商事件时可填)',
    external_id  VARCHAR(128)                                                        NULL COMMENT '承运商理赔编号',
    reason_code  ENUM ('LOST','DAMAGED','DELAY','OTHER')                             NOT NULL DEFAULT 'OTHER' COMMENT '理赔原因',
    status       ENUM ('FILED','UNDER_REVIEW','APPROVED','REJECTED','PAID','CLOSED') NOT NULL DEFAULT 'FILED' COMMENT '理赔状态',
    claim_amount BIGINT UNSIGNED                                                     NOT NULL COMMENT '理赔金额',
    paid_amount  BIGINT UNSIGNED                                                     NULL COMMENT '已支付金额',
    currency     CHAR(3)                                                             NOT NULL DEFAULT 'USD' COMMENT '币种',
    paid_at      DATETIME(3)                                                         NULL COMMENT '支付时间',
    created_at   DATETIME(3)                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_claim_external (external_id),
    KEY idx_claim_shipment (shipment_id),
    KEY idx_claim_order (order_id),
    KEY idx_claim_reship (reship_id),
    KEY idx_claim_ticket (ticket_id)
) ENGINE = InnoDB COMMENT ='承运商理赔单（独立于支付体系）';

-- =========================================================
-- 6. 售后领域 (customerservice)
-- =========================================================

-- 6.1 客服工单（售后/异常/理赔/补寄等统一入口）
/*
uk_ticket_no：对外/对内统一的工单编号（雪花/ULID），便于定位与对账
uk_ticket_open_dedupe：(user_id, order_id_nvl, shipment_id_nvl, issue_type, open_only) 保证同一用户在同一订单/包裹、同一问题类型下“进行中”仅有一张工单（避免并发重复；规避NULL绕过）
idx_ticket_user：用户侧列表
idx_ticket_order / idx_ticket_shipment / idx_ticket_orderitem：从订单/包裹/明细反查工单
idx_ticket_status_update：(status, updated_at) 轮询/看板扫描
idx_ticket_priority：(priority, created_at) 运营看板
idx_ticket_sla：SLA 到期预警
idx_ticket_assignee：坐席工作台（查看本人待处理）
idx_ticket_claim_ext：承运商理赔单号反查
CHECK：金额为正、必要时可进一步加严（按 issue_type 约束字段）
*/
CREATE TABLE cs_ticket
(
    id                      BIGINT UNSIGNED                        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    ticket_no               CHAR(26)                               NOT NULL COMMENT '工单编号(ULID/雪花等, 对外可见)',
    user_id                 BIGINT UNSIGNED                        NOT NULL COMMENT '发起用户ID, 指向 user_account.id',
    order_id                BIGINT UNSIGNED                        NULL COMMENT '关联订单, 指向 orders.id',
    order_item_id           BIGINT UNSIGNED                        NULL COMMENT '关联订单明细, 指向 order_item.id',
    shipment_id             BIGINT UNSIGNED                        NULL COMMENT '关联包裹, 指向 shipment.id',

    issue_type              ENUM ('REFUND','RESHIP','CLAIM','DELIVERY','ADDRESS','PRODUCT','PAYMENT','OTHER')
                                                                   NOT NULL DEFAULT 'OTHER' COMMENT '问题类型: 退款/补寄/理赔/物流/改址/商品/支付/其他',
    status                  ENUM ('OPEN','IN_PROGRESS','AWAITING_USER','AWAITING_CARRIER','ON_HOLD','RESOLVED','REJECTED','CLOSED')
                                                                   NOT NULL DEFAULT 'OPEN' COMMENT '工单状态机',
    priority                ENUM ('LOW','NORMAL','HIGH','URGENT')  NOT NULL DEFAULT 'NORMAL' COMMENT '优先级',
    channel                 ENUM ('CLIENT','ADMIN','API','SYSTEM') NOT NULL DEFAULT 'CLIENT' COMMENT '来源渠道: 前端/后台/开放API/系统',

    title                   VARCHAR(200)                           NOT NULL COMMENT '标题',
    `description`           VARCHAR(2000)                          NULL COMMENT '问题描述/背景',
    attachments             JSON                                   NULL COMMENT '附件(图片/文件)列表(JSON)',
    evidence                JSON                                   NULL COMMENT '证据/举证材料(JSON)',
    tags                    JSON                                   NULL COMMENT '业务标签(JSON数组)',

    -- 退款/理赔相关（可选字段）
    requested_refund_amount BIGINT UNSIGNED                        NULL COMMENT '申请退款金额(按订单币种)',
    currency                CHAR(3)                                NULL     DEFAULT 'USD' COMMENT '币种',
    claim_external_id       VARCHAR(128)                           NULL COMMENT '承运商理赔外部编号(存在则填)',

    -- 补寄相关（可选字段）
    reship_items            JSON                                   NULL COMMENT '补寄明细(含SKU与数量的快照)',

    -- 派工/进度
    assigned_to_user_id     BIGINT UNSIGNED                        NULL COMMENT '指派坐席用户ID',
    assigned_at             DATETIME(3)                            NULL COMMENT '指派时间',
    last_message_at         DATETIME(3)                            NULL COMMENT '最近用户/客服互动时间',
    sla_due_at              DATETIME(3)                            NULL COMMENT 'SLA 到期时间',
    resolved_at             DATETIME(3)                            NULL COMMENT '标记解决时间',
    closed_at               DATETIME(3)                            NULL COMMENT '关闭时间',

    -- 去重辅助键（修复NULL参与唯一键时的绕过问题）
    order_id_nvl            BIGINT UNSIGNED AS (IFNULL(order_id, 0)) STORED,
    shipment_id_nvl         BIGINT UNSIGNED AS (IFNULL(shipment_id, 0)) STORED,
    open_only               TINYINT AS (CASE
                                            WHEN status IN
                                                 ('OPEN', 'IN_PROGRESS', 'AWAITING_USER', 'AWAITING_CARRIER', 'ON_HOLD')
                                                THEN 1 END) STORED,

    source_ref              VARCHAR(128)                           NULL COMMENT '来源引用ID(如回调/任务run_id/请求id)',
    extra                   JSON                                   NULL COMMENT '扩展字段(冗余业务信息/调试)',

    created_at              DATETIME(3)                            NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at              DATETIME(3)                            NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),

    -- 仅对“进行中”状态做去重: 同一用户 + 同一订单/包裹 × 问题类型，仅允许一张开放工单
    UNIQUE KEY uk_ticket_open_dedupe (user_id, order_id_nvl, shipment_id_nvl, issue_type, open_only),
    KEY idx_ticket_user (user_id, created_at),
    KEY idx_ticket_order (order_id),
    KEY idx_ticket_orderitem (order_item_id),
    KEY idx_ticket_shipment (shipment_id),
    KEY idx_ticket_status_update (status, updated_at),
    KEY idx_ticket_priority (priority, created_at),
    KEY idx_ticket_sla (sla_due_at),
    KEY idx_ticket_assignee (assigned_to_user_id, status),
    KEY idx_ticket_claim_ext (claim_external_id),
    CHECK (requested_refund_amount IS NULL OR requested_refund_amount > 0)
) ENGINE = InnoDB COMMENT ='客服工单(售后/异常/理赔/补寄统一入口)';

-- 6.2 工单参与方（会话成员/已读位点）
/*
uk_ticket_participant：同一工单内，同类参与方 + 同一用户仅保留一条“活跃成员”记录
idx_participant_user_active：按用户查其参与中的工单（用户侧/坐席侧列表）
idx_participant_ticket：加载某工单全部参与方
*/
CREATE TABLE cs_ticket_participant
(
    id                      BIGINT UNSIGNED                                    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    ticket_id               BIGINT UNSIGNED                                    NOT NULL COMMENT '工单ID, 指向 cs_ticket.id',
    participant_type        ENUM ('USER','AGENT','MERCHANT','SYSTEM','BOT')    NOT NULL COMMENT '参与方类型',
    participant_user_id     BIGINT UNSIGNED                                    NULL COMMENT '参与用户ID, 指向 user_account.id; SYSTEM/BOT可空',
    role                    ENUM ('OWNER','ASSIGNEE','COLLABORATOR','WATCHER') NOT NULL DEFAULT 'OWNER' COMMENT '工单内角色',
    joined_at               DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入时间',
    left_at                 DATETIME(3)                                        NULL COMMENT '退出时间(空=仍在会话中)',
    last_read_message_id    BIGINT UNSIGNED                                    NULL COMMENT '最后已读消息ID, 指向 cs_ticket_message.id',
    last_read_at            DATETIME(3)                                        NULL COMMENT '最后已读时间',
    created_at              DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at              DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    participant_user_id_nvl BIGINT UNSIGNED AS (IFNULL(participant_user_id, 0)) STORED,
    active_only             TINYINT AS (CASE WHEN left_at IS NULL THEN 1 END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_participant (ticket_id, participant_type, participant_user_id_nvl, active_only),
    KEY idx_participant_user_active (participant_user_id, left_at, ticket_id),
    KEY idx_participant_ticket (ticket_id)
) ENGINE = InnoDB COMMENT ='工单参与方(成员/角色/已读位点)';

-- 6.3 工单消息（用户-客服实时会话）
/*
uk_msg_no：消息编号唯一（雪花/ULID）
uk_msg_client_dedupe：客户端消息ID幂等（弱网重试防重）
idx_msg_ticket_time：按工单时间线加载消息
idx_msg_sender_time：按发送方检索消息
CHECK：消息至少包含文本或附件之一
*/
CREATE TABLE cs_ticket_message
(
    id                 BIGINT UNSIGNED                                    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    message_no         CHAR(26)                                           NOT NULL COMMENT '消息编号(ULID/雪花等)',
    ticket_id          BIGINT UNSIGNED                                    NOT NULL COMMENT '工单ID, 指向 cs_ticket.id',
    sender_type        ENUM ('USER','AGENT','MERCHANT','SYSTEM','BOT')    NOT NULL COMMENT '发送方类型',
    sender_user_id     BIGINT UNSIGNED                                    NULL COMMENT '发送用户ID, 指向 user_account.id; SYSTEM/BOT可空',
    message_type       ENUM ('TEXT','IMAGE','FILE','SYSTEM_EVENT','RICH') NOT NULL DEFAULT 'TEXT' COMMENT '消息类型',
    content            VARCHAR(4000)                                      NULL COMMENT '消息内容(文本/系统提示文案)',
    attachments        JSON                                               NULL COMMENT '附件列表(JSON, 图片/文件等)',
    metadata           JSON                                               NULL COMMENT '扩展元数据(JSON)',
    client_message_id  VARCHAR(64)                                        NULL COMMENT '客户端消息ID(用于幂等重试)',
    sent_at            DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间',
    edited_at          DATETIME(3)                                        NULL COMMENT '编辑时间',
    recalled_at        DATETIME(3)                                        NULL COMMENT '撤回时间',
    created_at         DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at         DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    sender_user_id_nvl BIGINT UNSIGNED AS (IFNULL(sender_user_id, 0)) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_no (message_no),
    UNIQUE KEY uk_msg_client_dedupe (ticket_id, sender_type, sender_user_id_nvl, client_message_id),
    KEY idx_msg_ticket_time (ticket_id, sent_at, id),
    KEY idx_msg_sender_time (sender_user_id, sent_at),
    CHECK (content IS NOT NULL OR attachments IS NOT NULL)
) ENGINE = InnoDB COMMENT ='工单消息(用户与客服会话)';

-- 6.4 工单状态流转日志（状态机审计）
/*
idx_tsl_ticket_time：按工单回放状态时间线
idx_tsl_to_status：按目标状态统计与筛查
idx_tsl_actor：按操作者排查
*/
CREATE TABLE cs_ticket_status_log
(
    id            BIGINT UNSIGNED                                             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    ticket_id     BIGINT UNSIGNED                                             NOT NULL COMMENT '工单ID, 指向 cs_ticket.id',
    from_status   ENUM ('OPEN','IN_PROGRESS','AWAITING_USER','AWAITING_CARRIER','ON_HOLD','RESOLVED','REJECTED','CLOSED')
                                                                              NULL COMMENT '变更前状态',
    to_status     ENUM ('OPEN','IN_PROGRESS','AWAITING_USER','AWAITING_CARRIER','ON_HOLD','RESOLVED','REJECTED','CLOSED')
                                                                              NOT NULL COMMENT '变更后状态',
    actor_type    ENUM ('USER','AGENT','MERCHANT','SYSTEM','SCHEDULER','API') NOT NULL COMMENT '操作者类型',
    actor_user_id BIGINT UNSIGNED                                             NULL COMMENT '操作者用户ID, 系统动作可空',
    source_ref    VARCHAR(128)                                                NULL COMMENT '来源引用ID(请求id/任务run_id等)',
    note          VARCHAR(255)                                                NULL COMMENT '变更说明',
    created_at    DATETIME(3)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tsl_ticket_time (ticket_id, created_at),
    KEY idx_tsl_to_status (to_status, created_at),
    KEY idx_tsl_actor (actor_user_id, created_at)
) ENGINE = InnoDB COMMENT ='工单状态流转日志';

-- 6.5 工单指派日志（转派/认领审计）
/*
idx_tal_ticket_time：按工单回放指派历史
idx_tal_to_assignee：按被指派用户检索
idx_tal_actor：按操作者排查
*/
CREATE TABLE cs_ticket_assignment_log
(
    id                    BIGINT UNSIGNED                                             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    ticket_id             BIGINT UNSIGNED                                             NOT NULL COMMENT '工单ID, 指向 cs_ticket.id',
    from_assignee_user_id BIGINT UNSIGNED                                             NULL COMMENT '原指派用户ID, 指向 user_account.id',
    to_assignee_user_id   BIGINT UNSIGNED                                             NULL COMMENT '新指派用户ID, 指向 user_account.id',
    action_type           ENUM ('ASSIGN','REASSIGN','UNASSIGN','AUTO_ASSIGN')         NOT NULL COMMENT '指派动作',
    actor_type            ENUM ('USER','AGENT','MERCHANT','SYSTEM','SCHEDULER','API') NOT NULL COMMENT '操作者类型',
    actor_user_id         BIGINT UNSIGNED                                             NULL COMMENT '操作者用户ID',
    source_ref            VARCHAR(128)                                                NULL COMMENT '来源引用ID(请求id/任务run_id等)',
    note                  VARCHAR(255)                                                NULL COMMENT '备注',
    created_at            DATETIME(3)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tal_ticket_time (ticket_id, created_at),
    KEY idx_tal_to_assignee (to_assignee_user_id, created_at),
    KEY idx_tal_actor (actor_user_id, created_at)
) ENGINE = InnoDB COMMENT ='工单指派日志';

-- 6.6 补发单（登记补发记录）
/*
uk_reship_no：对外/对内统一的补发单编号（雪花/ULID）
idx_reship_order：从订单反查补发单
idx_reship_ticket：从客服工单反查补发单
 */
CREATE TABLE aftersales_reship
(
    id            BIGINT UNSIGNED                 NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    reship_no     CHAR(26)                        NOT NULL COMMENT '补发单号(ULID/雪花)',
    order_id      BIGINT UNSIGNED                 NOT NULL COMMENT '原订单ID, 指向 orders.id',
    ticket_id     BIGINT UNSIGNED                 NULL COMMENT '关联客服工单, 指向 cs_ticket.id',
    shipment_id   BIGINT UNSIGNED                 NOT NULL COMMENT '原物流单 ID, 指向 shipment.id',
    reason_code   ENUM ('LOST','DAMAGED','OTHER') NOT NULL DEFAULT 'OTHER' COMMENT '补发原因',
    status        ENUM ('INIT','APPROVED','FULFILLING','FULFILLED','CANCELLED')
                                                  NOT NULL DEFAULT 'INIT' COMMENT '补发状态',
    currency      CHAR(3)                         NOT NULL DEFAULT 'USD' COMMENT '币种(费用口径)',
    items_cost    BIGINT UNSIGNED                 NULL COMMENT '补发货品成本(可后置结算填充)',
    shipping_cost BIGINT UNSIGNED                 NULL COMMENT '补发运费成本',
    note          VARCHAR(255)                    NULL COMMENT '备注/原因',
    created_at    DATETIME(3)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reship_no (reship_no),
    KEY idx_reship_order (order_id),
    KEY idx_reship_ticket (ticket_id)
) ENGINE = InnoDB COMMENT ='售后补发单(登记补发记录)';

-- 6.7 补发明细（基于原订单明细做数量维度的补发）
/*
uk_reship_item_once：同一补发单内，同一订单明细只允许出现一次
idx_reship_item_reship：从补发单反查明细
idx_reship_item_orderitem：从订单明细反查明细
 */
CREATE TABLE aftersales_reship_item
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    reship_id     BIGINT UNSIGNED NOT NULL COMMENT '补发单ID, 指向 aftersales_reship.id',
    order_item_id BIGINT UNSIGNED NOT NULL COMMENT '原订单明细ID, 指向 order_item.id',
    sku_id        BIGINT UNSIGNED NOT NULL COMMENT 'SKU(冗余校验/统计), 指向 product_sku.id',
    quantity      INT             NOT NULL COMMENT '补发数量(>0)',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reship_item_once (reship_id, order_item_id), -- 单一补发单内防重复
    KEY idx_reship_item_reship (reship_id),
    KEY idx_reship_item_orderitem (order_item_id),
    CHECK (quantity > 0)
) ENGINE = InnoDB COMMENT ='售后补发单明细';

-- 6.8 补发-包裹 关联（一个补发单可产生1..N个shipment）
/*
uk_reship_one_shipment：一个包裹只能被一个补发单关联
idx_reship_ship_reship：从补发单反查包裹
idx_reship_ship_shipment：从包裹反查补发单
 */
CREATE TABLE aftersales_reship_shipment
(
    reship_id   BIGINT UNSIGNED NOT NULL COMMENT '补发单ID, 指向 aftersales_reship.id',
    shipment_id BIGINT UNSIGNED NOT NULL COMMENT '包裹ID, 指向 shipment.id',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (reship_id, shipment_id),
    UNIQUE KEY uk_reship_one_shipment (shipment_id), -- 避免一个包裹被多个补发单占用
    KEY idx_reship_ship_reship (reship_id),
    KEY idx_reship_ship_shipment (shipment_id)
) ENGINE = InnoDB COMMENT ='补发单-包裹关联';

-- =========================================================
-- 其他辅助表
-- =========================================================

-- 7.1 货币表
CREATE TABLE currency
(
    code              CHAR(3)        NOT NULL COMMENT 'ISO 4217 代码，如 USD/EUR/JPY',
    name              VARCHAR(64)    NOT NULL COMMENT '币种名称',
    symbol            VARCHAR(8)     NULL COMMENT '货币符号，如 $, €',
    minor_unit        TINYINT        NOT NULL DEFAULT 2 COMMENT '最小货币单位小数位，如 JPY=0, USD=2, KWD=3',
    cash_rounding_inc DECIMAL(10, 4) NULL COMMENT '现金舍入增量，如 CHF=0.05；为空表示按10^(-minor_unit)',
    rounding_mode     ENUM ('HALF_UP','HALF_DOWN','BANKERS','UP','DOWN')
                                     NOT NULL DEFAULT 'HALF_UP' COMMENT '默认舍入规则',
    enabled           TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (code)
) ENGINE = InnoDB COMMENT ='币种字典与舍入规则';

-- 7.2 语言字典（站点可用语言）
/*
uk_locale_default：利用 NULL 可重复 + 生成列，保证全站仅 1 个默认语言。
idx_locale_enabled：后台/页面按启用状态过滤语言列表。
*/
CREATE TABLE locale
(
    code        VARCHAR(16) NOT NULL COMMENT '语言代码，如 zh-CN, en, ja',
    name        VARCHAR(64) NOT NULL COMMENT '语言名称(英文)',
    native_name VARCHAR(64) NOT NULL COMMENT '本地语言名',
    enabled     TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否启用',
    is_default  TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否默认语言(全站唯一)',
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (code),
    default_one TINYINT AS (CASE WHEN is_default = 1 THEN 1 ELSE NULL END) STORED,
    UNIQUE KEY uk_locale_default (default_one),
    KEY idx_locale_enabled (enabled),
    CHECK (code REGEXP '^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?$')
) ENGINE = InnoDB COMMENT ='站点语言字典';


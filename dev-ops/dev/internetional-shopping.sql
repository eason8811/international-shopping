-- =========================================================
-- 基础：库与 SQL Mode
-- =========================================================
CREATE DATABASE IF NOT EXISTS shopdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE shopdb;

-- =========================================================
-- 1) 用户领域 (user)
-- =========================================================

-- 1.1 账户主表：本系统用户（JWT 认证）
/*
uk_user_username/email/phone：登录唯一性，避免重复注册，用于登录/找回帐号的等值查询
idx_user_status：后台用户列表按状态筛选
idx_user_last_login：近期登录活跃度排序/检索
*/
CREATE TABLE user_account
(
    id            BIGINT UNSIGNED            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username      VARCHAR(64)                NOT NULL COMMENT '用户名(登录名)',
    nickname      VARCHAR(64)                NOT NULL COMMENT '昵称/显示名',
    email         VARCHAR(255)               NULL COMMENT '邮箱(可空)',
    phone         VARCHAR(32)                NULL COMMENT '手机号(可空, 含区号需要统一格式)',
    password_hash VARCHAR(255)               NULL COMMENT '密码哈希(本地账号用; 第三方可空)',
    status        ENUM ('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
    last_login_at DATETIME(3)                NULL COMMENT '最近登录时间',
    is_deleted    TINYINT(1)                 NOT NULL DEFAULT 0 COMMENT '软删除标记(0否1是)',
    created_at    DATETIME(3)                NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)                NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_phone (phone),
    KEY idx_user_status (status),
    KEY idx_user_last_login (last_login_at)
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
    provider      ENUM ('LOCAL','GOOGLE', 'FACEBOOK', 'APPLE', 'INSTAGRAM', 'TIKTOK')
                                  NOT NULL COMMENT '认证提供方',
    provider_uid  VARCHAR(191)    NOT NULL COMMENT '提供方用户唯一ID(如openid/subject)',
    password_hash VARCHAR(255)    NULL COMMENT '本地账号密码哈希(仅provider=LOCAL需要)',
    access_token  VARBINARY(1024) NULL COMMENT '访问令牌(密文/加密保存)',
    refresh_token VARBINARY(1024) NULL COMMENT '刷新令牌(密文/加密保存)',
    expires_at    DATETIME(3)     NULL COMMENT '访问令牌过期时间',
    scope         VARCHAR(512)    NULL COMMENT '授权范围',
    last_login_at DATETIME(3)     NULL COMMENT '该通道最近登录时间',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_provider_uid (provider, provider_uid),
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
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID, 指向 user_account.id',
    receiver_name VARCHAR(64)     NOT NULL COMMENT '收货人',
    phone         VARCHAR(32)     NOT NULL COMMENT '联系电话',
    country       VARCHAR(64)     NULL COMMENT '国家',
    province      VARCHAR(64)     NULL COMMENT '省/州',
    city          VARCHAR(64)     NULL COMMENT '城市',
    district      VARCHAR(64)     NULL COMMENT '区/县',
    address_line1 VARCHAR(255)    NOT NULL COMMENT '地址行1',
    address_line2 VARCHAR(255)    NULL COMMENT '地址行2',
    zipcode       VARCHAR(20)     NULL COMMENT '邮编',
    is_default    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_addr_user (user_id),
    KEY idx_addr_user_default (user_id, is_default)
) ENGINE = InnoDB COMMENT ='用户收货地址';

-- =========================================================
-- 2) 商品领域 (product)
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
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    category_id  BIGINT UNSIGNED NOT NULL COMMENT '分类ID, 指向 product_category.id',
    locale       VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    name         VARCHAR(64)     NOT NULL COMMENT '分类名(本地化)',
    slug         VARCHAR(64)     NOT NULL COMMENT '分类slug(本地化, 用于多语言路由/SEO)',
    brand        VARCHAR(120)    NULL COMMENT '品牌文案(本地化, 按你要求放于分类i18n)',
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
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
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id   BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    locale       VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    title        VARCHAR(255)    NOT NULL COMMENT '标题(本地化)',
    subtitle     VARCHAR(255)    NULL COMMENT '副标题(本地化)',
    description  TEXT            NULL COMMENT '描述(本地化)',
    slug         VARCHAR(120)    NOT NULL COMMENT '商品slug(本地化, 用于多语言路由/SEO)',
    tags         JSON            NULL COMMENT '标签(本地化, 可用于站内搜索/推荐)',
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
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

-- 2.8 购物车(用户-SKU 映射)
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

-- 2.9 商品 Like (用户-商品 映射)
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

-- 2.10 库存日志（预占/扣减/释放）
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
    KEY idx_inv_sku_time (sku_id, created_at),
    KEY idx_inv_order (order_id)
) ENGINE = InnoDB COMMENT ='库存变动日志';

-- 2.11 SKU 多币种价格
/*
uk_price_sku_ccy：确保一个 sku 在一个结算货币中只对应一个售价
idx_price_ccy：按币种统计/导出
 */
CREATE TABLE product_price
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sku_id     BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    currency   CHAR(3)         NOT NULL COMMENT '币种, 指向 currency.code',
    list_price DECIMAL(18, 2)  NOT NULL COMMENT '标价 (含税口径由业务约定)',
    sale_price DECIMAL(18, 2)  NULL COMMENT '促销价 (可空, 为空表示无促销)',
    is_active  TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否可售用价 (预留)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_sku_ccy (sku_id, currency), -- 1:1 唯一价
    KEY idx_price_ccy (currency),                   -- 统计/导出
    CHECK (sale_price IS NULL OR sale_price <= list_price),
    CHECK (list_price > 0),
    CHECK (sale_price IS NULL OR sale_price > 0)
) ENGINE = InnoDB COMMENT ='SKU 多币种定价(上货即确定各币种价格)';

-- 2.12 SPU 规格类别
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

-- 2.13 规格类别 i18n（spec 名称多语言）
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

-- 2.14 SPU 规格值
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

-- 2.15 规格值 i18n（value 名称多语言）
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

-- 2.16 SKU-规格值映射
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

-- =========================================================
-- 3) 订单领域 (orders)
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
    id                  BIGINT UNSIGNED                                           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no            CHAR(26)                                                  NOT NULL COMMENT '业务单号(如ULID/雪花, 对外展示)',
    user_id             BIGINT UNSIGNED                                           NOT NULL COMMENT '下单用户ID, 指向 user_account.id',
    status              ENUM ('CREATED','PENDING_PAYMENT','PAID','CANCELLED','CLOSED','FULFILLED','REFUNDING','REFUNDED')
                                                                                  NOT NULL DEFAULT 'CREATED' COMMENT '订单状态机',
    items_count         INT                                                       NOT NULL DEFAULT 0 COMMENT '商品总件数',
    total_amount        DECIMAL(18, 2)                                            NOT NULL COMMENT '商品总额(未含运费/折扣)',
    discount_amount     DECIMAL(18, 2)                                            NOT NULL DEFAULT 0 COMMENT '折扣总额',
    shipping_amount     DECIMAL(18, 2)                                            NOT NULL DEFAULT 0 COMMENT '运费',
    pay_amount          DECIMAL(18, 2)                                            NOT NULL COMMENT '应付金额=总额-折扣+运费',
    currency            CHAR(3)                                                   NOT NULL DEFAULT 'CNY' COMMENT '币种',
    pay_channel         ENUM ('NONE','ALIPAY','WECHAT','STRIPE','PAYPAL','OTHER') NOT NULL DEFAULT 'NONE' COMMENT '支付通道',
    pay_status          ENUM ('NONE','INIT','SUCCESS','FAIL','CLOSED')            NOT NULL DEFAULT 'NONE' COMMENT '支付状态(网关侧)',
    payment_external_id VARCHAR(128)                                              NULL COMMENT '支付单 externalId(网关唯一标记)',
    pay_time            DATETIME(3)                                               NULL COMMENT '支付成功时间',
    address_snapshot    JSON                                                      NULL COMMENT '收货信息快照(JSON)',
    buyer_remark        VARCHAR(500)                                              NULL COMMENT '买家留言',
    cancel_reason       VARCHAR(255)                                              NULL COMMENT '取消原因',
    cancel_time         DATETIME(3)                                               NULL COMMENT '取消时间',
    created_at          DATETIME(3)                                               NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at          DATETIME(3)                                               NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id),
    KEY idx_order_status_time (status, created_at),
    KEY idx_order_created (created_at),
    KEY idx_order_payment_ext (payment_external_id)
) ENGINE = InnoDB COMMENT ='订单主表';

-- 3.2 订单明细
/*
idx_item_order：加载订单明细
idx_item_prod / idx_item_sku：基于商品/SKU 的售卖分析
 */
CREATE TABLE order_item
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT UNSIGNED NOT NULL COMMENT '订单 ID, 指向 orders.id',
    product_id      BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    sku_id          BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    title           VARCHAR(255)    NOT NULL COMMENT '商品标题快照',
    sku_attrs       JSON            NULL COMMENT 'SKU属性快照(JSON)',
    cover_image_url VARCHAR(500)    NULL COMMENT '商品图快照',
    unit_price      DECIMAL(18, 2)  NOT NULL COMMENT '单价快照',
    quantity        INT             NOT NULL COMMENT '数量',
    subtotal_amount DECIMAL(18, 2)  NOT NULL COMMENT '小计=单价*数量',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_item_order (order_id),
    KEY idx_item_prod (product_id),
    KEY idx_item_sku (sku_id)
) ENGINE = InnoDB COMMENT ='订单明细';

-- 3.3 支付单（对接支付API，使用 externalId 关联）  /* 待定, 视支付通道 API 文档而定 */
/*
uk_pay_external：支付网关 externalId 唯一（幂等与关联）
idx_pay_order：从订单查支付单
idx_pay_status_update (status, updated_at)：按状态+时间扫描（轮询重试/清理）
 */
CREATE TABLE payment_order
(
    id               BIGINT UNSIGNED                                    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id         BIGINT UNSIGNED                                    NOT NULL COMMENT '订单ID, 指向 orders.id',
    external_id      VARCHAR(128)                                       NOT NULL COMMENT '支付网关 externalId(唯一)',
    channel          ENUM ('ALIPAY','WECHAT','STRIPE','PAYPAL','OTHER') NOT NULL COMMENT '支付通道',
    amount           DECIMAL(18, 2)                                     NOT NULL COMMENT '支付金额',
    currency         CHAR(3)                                            NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status           ENUM ('INIT','PENDING','SUCCESS','FAIL','CLOSED')  NOT NULL DEFAULT 'INIT' COMMENT '支付单状态',
    request_payload  JSON                                               NULL COMMENT '下单请求报文(JSON)',
    response_payload JSON                                               NULL COMMENT '下单响应报文(JSON)',
    notify_payload   JSON                                               NULL COMMENT '最近一次回调报文(JSON)',
    last_polled_at   DATETIME(3)                                        NULL COMMENT '最近轮询时间',
    last_notified_at DATETIME(3)                                        NULL COMMENT '最近回调处理时间',
    created_at       DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at       DATETIME(3)                                        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_external (external_id),
    KEY idx_pay_order (order_id),
    KEY idx_pay_status_update (status, updated_at)
) ENGINE = InnoDB COMMENT ='支付单(网关externalId对应)';

-- 3.4 订单状态流转日志（状态机审计）
/*
idx_osl_order：订单状态流转历史查询
 */
CREATE TABLE order_status_log
(
    id           BIGINT UNSIGNED                                               NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id     BIGINT UNSIGNED                                               NOT NULL COMMENT '订单ID, 指向 orders.id',
    event_source ENUM ('SYSTEM','USER','PAYMENT_CALLBACK','SCHEDULER','ADMIN') NOT NULL DEFAULT 'SYSTEM' COMMENT '事件来源',
    from_status  VARCHAR(32)                                                   NULL COMMENT '源状态',
    to_status    VARCHAR(32)                                                   NOT NULL COMMENT '目标状态',
    note         VARCHAR(255)                                                  NULL COMMENT '备注',
    created_at   DATETIME(3)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_osl_order (order_id)
) ENGINE = InnoDB COMMENT ='订单状态流转日志';

-- 3.5 物流包裹（一个包裹对应一张国际运单；支持后续末端换单）
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
    carrier_code    VARCHAR(64)     NOT NULL COMMENT '承运商编码(标准化如 dhl, ups, 4px, yanwen)',
    carrier_name    VARCHAR(128)    NULL COMMENT '承运商名称',
    service_code    VARCHAR(64)     NULL COMMENT '服务/产品代码(如 DHL_ECOM)',
    tracking_no     VARCHAR(128)    NULL COMMENT '承运商运单号/追踪号(可能下单后才生成)',
    ext_external_id VARCHAR(128)    NULL COMMENT '对接物流API的externalId/订单号',
    /*
     CREATED: 系统内运单已创建, 未向承运商下单（未拿到面单号）
     LABEL_CREATED: 已经拿到面单号
     PICKED_UP: 已被揽收
     IN_TRANSIT: 运输中
     CUSTOMS_HOLD: 海关暂扣
     CUSTOMS_RELEASED: 清关放行
     OUT_FOR_DELIVERY: 派送中
     DELIVERED: 签收完成（终态）
     EXCEPTION: 异常（可为终态或中间态，后续可能被重派，回退，或客户改地址后继续流转）
     RETURNED: 退回（终态）
     CANCELLED: 取消（面单作废，终态）
     LOST: 丢失（由承运商判定或长时间无轨迹且调度认定丢失，终态）
     */
    status          ENUM ('CREATED','LABEL_CREATED','PICKED_UP','IN_TRANSIT','CUSTOMS_HOLD','CUSTOMS_RELEASED',
        'OUT_FOR_DELIVERY','DELIVERED','EXCEPTION','RETURNED','CANCELLED','LOST')
                                    NOT NULL DEFAULT 'CREATED' COMMENT '物流状态',
    ship_from       JSON            NULL COMMENT '发货地/仓信息快照(JSON)',
    ship_to         JSON            NULL COMMENT '收件地址快照(JSON); 与订单快照可能不同',
    weight_kg       DECIMAL(10, 3)  NULL COMMENT '毛重(kg)',
    length_cm       DECIMAL(10, 1)  NULL COMMENT '长(cm)',
    width_cm        DECIMAL(10, 1)  NULL COMMENT '宽(cm)',
    height_cm       DECIMAL(10, 1)  NULL COMMENT '高(cm)',
    declared_value  DECIMAL(18, 2)  NULL COMMENT '申报价值(币种见currency)',
    currency        CHAR(3)         NOT NULL DEFAULT 'CNY' COMMENT '币种',
    customs_info    JSON            NULL COMMENT '关务/清关信息(HS code, 原产地, 税费等)',
    label_url       VARCHAR(500)    NULL COMMENT '电子面单URL(可选)',
    pickup_time     DATETIME(3)     NULL COMMENT '揽收时间',
    delivered_time  DATETIME(3)     NULL COMMENT '签收时间',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shipment_no (shipment_no),
    UNIQUE KEY uk_carrier_tracking (carrier_code, tracking_no), -- 同一承运商下运单号唯一
    UNIQUE KEY uk_ship_external (ext_external_id),              -- 对接方externalId确保幂等
    KEY idx_ship_order (order_id),                              -- 从订单查包裹
    KEY idx_ship_status_updated (status, updated_at),           -- 任务轮询/看板扫描
    KEY idx_ship_created (created_at)                           -- 时间窗口扫描
) ENGINE = InnoDB COMMENT ='物流包裹/运单(跨境)';

-- 3.6 包裹内商品映射（支持合单/拆单：N:N）
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

-- 3.7 包裹状态流转日志（源状态→目标状态 + 事件来源）
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
    from_status   ENUM ('CREATED','LABEL_CREATED','PICKED_UP','IN_TRANSIT','CUSTOMS_HOLD','CUSTOMS_RELEASED',
        'OUT_FOR_DELIVERY','DELIVERED','EXCEPTION','RETURNED','CANCELLED','LOST')
                                  NULL COMMENT '变更前状态 (首个状态可为空)',
    to_status     ENUM ('CREATED','LABEL_CREATED','PICKED_UP','IN_TRANSIT','CUSTOMS_HOLD','CUSTOMS_RELEASED',
        'OUT_FOR_DELIVERY','DELIVERED','EXCEPTION','RETURNED','CANCELLED','LOST')
                                  NOT NULL COMMENT '变更后状态',
    -- 事件发生时间与来源（谁触发, 如何触发）
    event_time    DATETIME(3)     NULL COMMENT '状态发生时间 (承运商/系统口径，空则参考created_at)',
    source_type   ENUM ('CARRIER_WEBHOOK','CARRIER_POLL','SYSTEM_JOB','MANUAL','API')
                                  NOT NULL COMMENT '事件来源类型：承运商回调/轮询/系统任务/人工/开放接口',
    source_ref    VARCHAR(128)    NULL COMMENT '来源引用ID (如回调notify_id, 任务run_id, 请求id)',
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
    KEY idx_ssl_source (source_type, created_at),                            -- 分来源按时间筛查

    -- 约束：from/to 不相等（MySQL CHECK 在 8/9 可用，但无法跨NULL严格校验）
    CHECK (from_status IS NULL OR from_status <> to_status)
) ENGINE = InnoDB COMMENT ='包裹状态流转日志 (源状态→目标状态，含事件来源与原始报文)';

-- =========================================================
-- 其他辅助表
-- =========================================================

-- 4.1 货币表
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

-- 4.2 语言字典（站点可用语言）
/*
uk_locale_default：利用 NULL 可重复 + 生成列，保证全站仅 1 个默认语言。
idx_locale_enabled：后台/页面按启用状态过滤语言列表。
*/
CREATE TABLE locale
(
    code        VARCHAR(16)  NOT NULL COMMENT '语言代码，如 zh-CN, en, ja',
    name        VARCHAR(64)  NOT NULL COMMENT '语言名称(英文)',
    native_name VARCHAR(64)  NOT NULL COMMENT '本地语言名',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    is_default  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认语言(全站唯一)',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (code),
    default_one TINYINT AS (CASE WHEN is_default=1 THEN 1 ELSE NULL END) STORED,
    UNIQUE KEY uk_locale_default (default_one),
    KEY idx_locale_enabled (enabled),
    CHECK (code REGEXP '^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?$')
) ENGINE = InnoDB COMMENT ='站点语言字典';





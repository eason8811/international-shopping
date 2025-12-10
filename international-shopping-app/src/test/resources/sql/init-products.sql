-- 基础用户表 (仅为满足外键字段, 不参与测试逻辑)
CREATE TABLE IF NOT EXISTS user_account
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username        VARCHAR(64)     NOT NULL COMMENT '用户名',
    email           VARCHAR(128)    NOT NULL COMMENT '邮箱',
    password_hash   VARCHAR(255)    NOT NULL COMMENT '密码哈希',
    salt            VARCHAR(64)     NULL COMMENT '密码盐值',
    status          ENUM ('PENDING','ACTIVE','DISABLED') NOT NULL DEFAULT 'PENDING' COMMENT '账号状态',
    activation_code VARCHAR(64)     NULL COMMENT '激活码',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB COMMENT ='用户账号';

-- 商品分类
CREATE TABLE IF NOT EXISTS product_category
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

-- 商品分类 i18n
CREATE TABLE IF NOT EXISTS product_category_i18n
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

-- 商品SPU
CREATE TABLE IF NOT EXISTS product
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
    default_sku_id  BIGINT UNSIGNED                                NULL COMMENT '默认展示SKU, 指向 product_sku.id',
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

-- 商品 SPU i18n
CREATE TABLE IF NOT EXISTS product_i18n
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

-- 商品SKU
CREATE TABLE IF NOT EXISTS product_sku
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

-- SPU 图片
CREATE TABLE IF NOT EXISTS product_image
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

-- SKU 图片
CREATE TABLE IF NOT EXISTS product_sku_image
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

-- 商品 Like
CREATE TABLE IF NOT EXISTS product_like
(
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户ID, 指向 user_account.id',
    product_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID, 指向 product.id',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Like 时间',
    PRIMARY KEY (user_id, product_id),
    KEY idx_like_product (product_id)
) ENGINE = InnoDB COMMENT ='商品 Like 关系';

-- SKU 多币种价格
CREATE TABLE IF NOT EXISTS product_price
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
    UNIQUE KEY uk_price_sku_ccy (sku_id, currency),
    KEY idx_price_ccy (currency),
    CHECK (sale_price IS NULL OR sale_price <= list_price),
    CHECK (list_price > 0),
    CHECK (sale_price IS NULL OR sale_price > 0)
) ENGINE = InnoDB COMMENT ='SKU 多币种定价(上货即确定各币种价格)';

-- SPU 规格类别
CREATE TABLE IF NOT EXISTS product_spec
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

-- 规格类别 i18n
CREATE TABLE IF NOT EXISTS product_spec_i18n
(
    spec_id    BIGINT UNSIGNED NOT NULL COMMENT '规格类别ID, 指向 product_spec.id',
    locale     VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    spec_name  VARCHAR(64)     NOT NULL COMMENT '规格类别名(本地化)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (spec_id, locale),
    KEY idx_spec_i18n_loc (locale)
) ENGINE = InnoDB COMMENT ='规格类别多语言';

-- SPU 规格值
CREATE TABLE IF NOT EXISTS product_spec_value
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

-- 规格值 i18n
CREATE TABLE IF NOT EXISTS product_spec_value_i18n
(
    value_id   BIGINT UNSIGNED NOT NULL COMMENT '规格值ID, 指向 product_spec_value.id',
    locale     VARCHAR(16)     NOT NULL COMMENT '语言代码, 指向 locale.code',
    value_name VARCHAR(64)     NOT NULL COMMENT '规格值名(本地化)',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (value_id, locale),
    KEY idx_specval_i18n_loc (locale)
) ENGINE = InnoDB COMMENT ='规格值多语言';

-- SKU-规格值映射
CREATE TABLE IF NOT EXISTS product_sku_spec
(
    sku_id     BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID, 指向 product_sku.id',
    spec_id    BIGINT UNSIGNED NOT NULL COMMENT '规格类别ID, 指向 product_spec.id',
    value_id   BIGINT UNSIGNED NOT NULL COMMENT '规格值ID, 指向 product_spec_value.id',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (sku_id, spec_id),
    KEY idx_pss_value (value_id),
    KEY idx_pss_spec_value (spec_id, value_id)
) ENGINE = InnoDB COMMENT ='SKU-规格值映射(每类别恰好一值)';

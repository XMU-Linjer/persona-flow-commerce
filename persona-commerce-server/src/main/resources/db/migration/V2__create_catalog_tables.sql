CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    `level` INT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    icon_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_category_parent_id (parent_id),
    KEY idx_product_category_status (status),
    KEY idx_product_category_level_sort (`level`, sort_order),
    CONSTRAINT fk_product_category_parent
        FOREIGN KEY (parent_id) REFERENCES product_category (id),
    CONSTRAINT chk_product_category_level
        CHECK (`level` IN (1, 2)),
    CONSTRAINT chk_product_category_status
        CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_spu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    subtitle VARCHAR(300) NULL,
    brand VARCHAR(100) NULL,
    description TEXT NULL,
    main_image_url VARCHAR(500) NULL,
    detail_images_json TEXT NULL,
    attributes_json TEXT NULL,
    tags VARCHAR(300) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_spu_category_id (category_id),
    KEY idx_product_spu_status (status),
    KEY idx_product_spu_name (name),
    KEY idx_product_spu_category_status_sort (category_id, status, sort_order),
    CONSTRAINT fk_product_spu_category
        FOREIGN KEY (category_id) REFERENCES product_category (id),
    CONSTRAINT chk_product_spu_status
        CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT NOT NULL AUTO_INCREMENT,
    spu_id BIGINT NOT NULL,
    sku_name VARCHAR(200) NOT NULL,
    specs_json TEXT NULL,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2) NULL,
    image_url VARCHAR(500) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    sales_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_sku_spu_id (spu_id),
    KEY idx_product_sku_status (status),
    KEY idx_product_sku_spu_status (spu_id, status),
    CONSTRAINT fk_product_sku_spu
        FOREIGN KEY (spu_id) REFERENCES product_spu (id),
    CONSTRAINT chk_product_sku_status
        CHECK (status IN (0, 1)),
    CONSTRAINT chk_product_sku_price
        CHECK (price >= 0),
    CONSTRAINT chk_product_sku_original_price
        CHECK (original_price IS NULL OR original_price >= 0),
    CONSTRAINT chk_product_sku_sales_count
        CHECK (sales_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO product_category (id, name, parent_id, `level`, sort_order, status, icon_url)
VALUES
    (1, '数码配件', NULL, 1, 10, 1, 'https://cdn.personaflow.local/catalog/categories/digital.png'),
    (2, '电脑办公', NULL, 1, 20, 1, 'https://cdn.personaflow.local/catalog/categories/office.png'),
    (3, '家居生活', NULL, 1, 30, 1, 'https://cdn.personaflow.local/catalog/categories/home.png'),
    (4, '运动户外', NULL, 1, 40, 1, 'https://cdn.personaflow.local/catalog/categories/outdoor.png'),
    (5, '食品饮料', NULL, 1, 50, 1, 'https://cdn.personaflow.local/catalog/categories/food.png'),
    (101, '耳机音频', 1, 2, 11, 1, 'https://cdn.personaflow.local/catalog/categories/audio.png'),
    (102, '充电存储', 1, 2, 12, 1, 'https://cdn.personaflow.local/catalog/categories/storage.png'),
    (201, '键盘鼠标', 2, 2, 21, 1, 'https://cdn.personaflow.local/catalog/categories/keyboard-mouse.png'),
    (202, '显示器办公', 2, 2, 22, 1, 'https://cdn.personaflow.local/catalog/categories/monitor-office.png'),
    (301, '家具收纳', 3, 2, 31, 1, 'https://cdn.personaflow.local/catalog/categories/furniture.png'),
    (302, '厨房日用', 3, 2, 32, 1, 'https://cdn.personaflow.local/catalog/categories/kitchen.png'),
    (401, '健身训练', 4, 2, 41, 1, 'https://cdn.personaflow.local/catalog/categories/fitness.png'),
    (402, '户外装备', 4, 2, 42, 1, 'https://cdn.personaflow.local/catalog/categories/outdoor-gear.png'),
    (501, '咖啡茶饮', 5, 2, 51, 1, 'https://cdn.personaflow.local/catalog/categories/coffee-tea.png'),
    (502, '休闲零食', 5, 2, 52, 1, 'https://cdn.personaflow.local/catalog/categories/snacks.png');

INSERT INTO product_spu (
    id,
    category_id,
    name,
    subtitle,
    brand,
    description,
    main_image_url,
    detail_images_json,
    attributes_json,
    tags,
    status,
    sort_order
)
VALUES
    (20001, 101, 'AuraSound CloudPods Pro 2 真无线降噪耳机', '通勤办公双场景降噪，支持空间音频', 'AuraSound', '轻量化入耳设计，适合通勤、办公会议和日常音乐收听。', 'https://cdn.personaflow.local/catalog/products/cloudpods-pro-2/main.jpg', '["https://cdn.personaflow.local/catalog/products/cloudpods-pro-2/detail-1.jpg","https://cdn.personaflow.local/catalog/products/cloudpods-pro-2/detail-2.jpg"]', '{"连接方式":"蓝牙 5.3","降噪":"主动降噪","续航":"最长 30 小时","适用场景":"通勤,办公,学习"}', '降噪,通勤,蓝牙,高续航', 1, 101),
    (20002, 101, 'SoundBar Mini 便携蓝牙音箱', '小体积大声场，户外露营也能用', 'SoundBar', '防泼溅机身和低频增强单元，适合桌面、厨房和轻户外使用。', 'https://cdn.personaflow.local/catalog/products/soundbar-mini/main.jpg', '["https://cdn.personaflow.local/catalog/products/soundbar-mini/detail-1.jpg"]', '{"连接方式":"蓝牙/AUX","防护等级":"IPX5","续航":"约 12 小时","适用场景":"桌面,厨房,露营"}', '蓝牙音箱,露营,桌面', 1, 102),
    (20003, 102, 'Voltix GaN 65W 三口快充充电器', '笔记本、平板、手机一个充电器搞定', 'Voltix', '采用 GaN 芯片，双 USB-C 加 USB-A 输出，适合差旅和桌面收纳。', 'https://cdn.personaflow.local/catalog/products/voltix-gan-65w/main.jpg', '["https://cdn.personaflow.local/catalog/products/voltix-gan-65w/detail-1.jpg"]', '{"最大功率":"65W","接口":"2C1A","协议":"PD/QC/PPS","适用设备":"手机,平板,轻薄本"}', '快充,GaN,差旅,桌面', 1, 103),
    (20004, 102, 'DataPocket Pro 1TB 移动固态硬盘', '高速备份，视频素材随身带', 'DataPocket', '金属外壳与 USB 3.2 Gen2 接口，适合摄影、设计和办公备份。', 'https://cdn.personaflow.local/catalog/products/datapocket-pro-1tb/main.jpg', '["https://cdn.personaflow.local/catalog/products/datapocket-pro-1tb/detail-1.jpg"]', '{"容量":"1TB","接口":"USB-C","读取速度":"最高 1050MB/s","适用场景":"摄影,剪辑,办公备份"}', '移动硬盘,高速,摄影,办公', 1, 104),
    (20005, 201, 'KeyForge K3 机械键盘', '紧凑 84 键布局，办公游戏两相宜', 'KeyForge', '支持多设备连接和热插拔轴体，兼顾敲击手感与桌面空间。', 'https://cdn.personaflow.local/catalog/products/keyforge-k3/main.jpg', '["https://cdn.personaflow.local/catalog/products/keyforge-k3/detail-1.jpg","https://cdn.personaflow.local/catalog/products/keyforge-k3/detail-2.jpg"]', '{"布局":"84 键","连接方式":"蓝牙/2.4G/有线","轴体":"可热插拔","适用场景":"办公,学习,游戏"}', '机械键盘,办公,游戏,多设备', 1, 201),
    (20006, 201, 'SilentPro M8 无线静音鼠标', '轻按静音，适合办公室和图书馆', 'SilentPro', '人体工学曲线与长续航电池，适合长时间办公。', 'https://cdn.personaflow.local/catalog/products/silentpro-m8/main.jpg', '["https://cdn.personaflow.local/catalog/products/silentpro-m8/detail-1.jpg"]', '{"连接方式":"2.4G/蓝牙","按键":"静音微动","DPI":"800-3200","适用场景":"办公,学习"}', '静音鼠标,办公,无线', 1, 202),
    (20007, 202, 'ViewMate 27 英寸 4K 显示器', '细腻画面，适合设计、代码和影音', 'ViewMate', '4K IPS 面板，支持低蓝光和可升降支架，提升桌面生产力。', 'https://cdn.personaflow.local/catalog/products/viewmate-27-4k/main.jpg', '["https://cdn.personaflow.local/catalog/products/viewmate-27-4k/detail-1.jpg"]', '{"尺寸":"27 英寸","分辨率":"3840x2160","面板":"IPS","适用场景":"设计,代码,影音"}', '4K显示器,办公,设计,护眼', 1, 203),
    (20008, 202, 'ErgoLift 铝合金笔记本支架', '抬高视线，改善桌面姿态', 'ErgoLift', '可折叠铝合金结构，适合居家办公和移动办公。', 'https://cdn.personaflow.local/catalog/products/ergolift-stand/main.jpg', '["https://cdn.personaflow.local/catalog/products/ergolift-stand/detail-1.jpg"]', '{"材质":"铝合金","适配":"11-17 英寸笔记本","角度":"多档可调","适用场景":"居家办公,差旅"}', '笔记本支架,人体工学,办公', 1, 204),
    (20009, 301, 'HomeEase E7 人体工学椅', '腰背支撑可调，久坐更舒适', 'HomeEase', '可调节头枕、腰托和扶手，适合长时间办公学习。', 'https://cdn.personaflow.local/catalog/products/homeease-e7/main.jpg', '["https://cdn.personaflow.local/catalog/products/homeease-e7/detail-1.jpg","https://cdn.personaflow.local/catalog/products/homeease-e7/detail-2.jpg"]', '{"支撑":"头枕/腰托/扶手可调","承重":"约 120kg","材质":"透气网布","适用场景":"办公,学习,电竞"}', '人体工学椅,久坐,办公', 1, 301),
    (20010, 301, 'LinenNest 纯棉四件套', '柔软亲肤，四季可用', 'LinenNest', '高支高密纯棉面料，适合卧室换季和新家布置。', 'https://cdn.personaflow.local/catalog/products/linennest-cotton-set/main.jpg', '["https://cdn.personaflow.local/catalog/products/linennest-cotton-set/detail-1.jpg"]', '{"面料":"纯棉","件数":"四件套","适用床型":"1.5m/1.8m","适用季节":"四季"}', '床品,纯棉,家居', 1, 302),
    (20011, 302, 'PureSteam S1 手持挂烫机', '快速除皱，出门前整理衣物', 'PureSteam', '轻量机身和快速出汽设计，适合衬衫、西装和旅行衣物护理。', 'https://cdn.personaflow.local/catalog/products/puresteam-s1/main.jpg', '["https://cdn.personaflow.local/catalog/products/puresteam-s1/detail-1.jpg"]', '{"功率":"1200W","水箱":"180ml","出汽时间":"约 25 秒","适用场景":"通勤,旅行,家用"}', '挂烫机,衣物护理,旅行', 1, 303),
    (20012, 302, 'FreshLock 玻璃保鲜盒套装', '分装备餐，冰箱收纳更清爽', 'FreshLock', '耐热玻璃盒体和密封盖，适合备餐、带饭和厨房收纳。', 'https://cdn.personaflow.local/catalog/products/freshlock-glass-box/main.jpg', '["https://cdn.personaflow.local/catalog/products/freshlock-glass-box/detail-1.jpg"]', '{"材质":"高硼硅玻璃","件数":"5 件套","适用":"微波炉/冰箱","适用场景":"备餐,带饭,收纳"}', '保鲜盒,厨房,备餐', 1, 304),
    (20013, 401, 'RunFlex Air 缓震跑鞋', '轻量回弹，日常慢跑训练适用', 'RunFlex', '透气鞋面与缓震中底，适合 3-10 公里日常跑步。', 'https://cdn.personaflow.local/catalog/products/runflex-air/main.jpg', '["https://cdn.personaflow.local/catalog/products/runflex-air/detail-1.jpg"]', '{"鞋面":"工程网布","中底":"轻量缓震","适用距离":"3-10 公里","适用场景":"慢跑,健身,通勤"}', '跑鞋,缓震,健身,通勤', 1, 401),
    (20014, 401, 'CoreFit TPE 加厚瑜伽垫', '防滑回弹，居家训练更稳定', 'CoreFit', '环保 TPE 材质，适合瑜伽、普拉提和拉伸训练。', 'https://cdn.personaflow.local/catalog/products/corefit-yoga-mat/main.jpg', '["https://cdn.personaflow.local/catalog/products/corefit-yoga-mat/detail-1.jpg"]', '{"材质":"TPE","厚度":"6mm/8mm","特性":"防滑,回弹","适用场景":"瑜伽,普拉提,拉伸"}', '瑜伽垫,居家健身,防滑', 1, 402),
    (20015, 402, 'TrailPeak 轻量防风冲锋衣', '城市通勤和轻户外都能穿', 'TrailPeak', '防风面料与可调节帽檐，适合徒步、露营和换季通勤。', 'https://cdn.personaflow.local/catalog/products/trailpeak-jacket/main.jpg', '["https://cdn.personaflow.local/catalog/products/trailpeak-jacket/detail-1.jpg"]', '{"面料":"防风防泼水","重量":"约 420g","适用场景":"徒步,露营,通勤","季节":"春秋"}', '冲锋衣,徒步,露营,通勤', 1, 403),
    (20016, 402, 'HydroTrail 316 不锈钢运动水壶', '保冷保温，健身和户外随身带', 'HydroTrail', '316 不锈钢内胆和防漏杯盖，适合健身房、骑行和登山。', 'https://cdn.personaflow.local/catalog/products/hydrotrail-bottle/main.jpg', '["https://cdn.personaflow.local/catalog/products/hydrotrail-bottle/detail-1.jpg"]', '{"材质":"316 不锈钢","容量":"600ml/900ml","保温":"约 12 小时","适用场景":"健身,骑行,登山"}', '运动水壶,保温,户外', 1, 404),
    (20017, 501, 'Mountain Roast 精品咖啡豆', '中度烘焙，坚果和黑巧风味', 'Mountain Roast', '精选阿拉比卡咖啡豆，适合手冲、美式和意式拼配。', 'https://cdn.personaflow.local/catalog/products/mountain-roast-beans/main.jpg', '["https://cdn.personaflow.local/catalog/products/mountain-roast-beans/detail-1.jpg"]', '{"产区":"云南/哥伦比亚拼配","烘焙度":"中度","风味":"坚果,黑巧,焦糖","适用":"手冲,美式,意式"}', '咖啡豆,手冲,办公室,早餐', 1, 501),
    (20018, 501, 'CalmLeaf 原叶乌龙茶', '清香回甘，冷热泡皆宜', 'CalmLeaf', '独立小袋包装，适合办公室、旅行和日常茶饮。', 'https://cdn.personaflow.local/catalog/products/calmleaf-oolong/main.jpg', '["https://cdn.personaflow.local/catalog/products/calmleaf-oolong/detail-1.jpg"]', '{"茶类":"乌龙茶","包装":"独立小袋","口感":"清香,回甘","适用场景":"办公室,旅行,日常"}', '乌龙茶,茶饮,办公室', 1, 502),
    (20019, 502, 'NutriMix 每日坚果 30 袋装', '坚果果干科学配比，轻负担加餐', 'NutriMix', '独立小包装，适合作为办公室、健身后和早餐加餐。', 'https://cdn.personaflow.local/catalog/products/nutrimix-nuts/main.jpg', '["https://cdn.personaflow.local/catalog/products/nutrimix-nuts/detail-1.jpg"]', '{"规格":"30 袋","成分":"坚果,果干","口味":"原味轻烘","适用场景":"办公室,健身,早餐"}', '每日坚果,零食,健身,早餐', 1, 503),
    (20020, 502, 'BerryOat 燕麦能量棒', '莓果酸甜，运动前后快速补能', 'BerryOat', '燕麦、莓果和坚果组合，适合通勤、户外和运动补给。', 'https://cdn.personaflow.local/catalog/products/berryoat-bar/main.jpg', '["https://cdn.personaflow.local/catalog/products/berryoat-bar/detail-1.jpg"]', '{"规格":"12 条","口味":"莓果燕麦","能量":"约 170kcal/条","适用场景":"通勤,户外,运动"}', '能量棒,燕麦,运动补给', 1, 504);

INSERT INTO product_sku (
    id,
    spu_id,
    sku_name,
    specs_json,
    price,
    original_price,
    image_url,
    status,
    sales_count
)
VALUES
    (30001, 20001, 'CloudPods Pro 2 云白', '{"颜色":"云白","版本":"标准版"}', 699.00, 799.00, 'https://cdn.personaflow.local/catalog/products/cloudpods-pro-2/sku-white.jpg', 1, 1280),
    (30002, 20001, 'CloudPods Pro 2 曜石黑', '{"颜色":"曜石黑","版本":"标准版"}', 699.00, 799.00, 'https://cdn.personaflow.local/catalog/products/cloudpods-pro-2/sku-black.jpg', 1, 1068),
    (30003, 20002, 'SoundBar Mini 雾灰', '{"颜色":"雾灰","功率":"12W"}', 199.00, 249.00, 'https://cdn.personaflow.local/catalog/products/soundbar-mini/sku-gray.jpg', 1, 823),
    (30004, 20002, 'SoundBar Mini 松石绿', '{"颜色":"松石绿","功率":"12W"}', 209.00, 259.00, 'https://cdn.personaflow.local/catalog/products/soundbar-mini/sku-green.jpg', 1, 618),
    (30005, 20003, 'Voltix GaN 65W 白色', '{"颜色":"白色","接口":"2C1A","功率":"65W"}', 159.00, 199.00, 'https://cdn.personaflow.local/catalog/products/voltix-gan-65w/sku-white.jpg', 1, 1576),
    (30006, 20003, 'Voltix GaN 65W 黑色', '{"颜色":"黑色","接口":"2C1A","功率":"65W"}', 159.00, 199.00, 'https://cdn.personaflow.local/catalog/products/voltix-gan-65w/sku-black.jpg', 1, 1322),
    (30007, 20004, 'DataPocket Pro 1TB 银色', '{"容量":"1TB","颜色":"银色"}', 599.00, 699.00, 'https://cdn.personaflow.local/catalog/products/datapocket-pro-1tb/sku-silver.jpg', 1, 486),
    (30008, 20004, 'DataPocket Pro 1TB 深空灰', '{"容量":"1TB","颜色":"深空灰"}', 599.00, 699.00, 'https://cdn.personaflow.local/catalog/products/datapocket-pro-1tb/sku-gray.jpg', 1, 529),
    (30009, 20005, 'KeyForge K3 青轴 白色', '{"轴体":"青轴","颜色":"白色","连接":"三模"}', 459.00, 529.00, 'https://cdn.personaflow.local/catalog/products/keyforge-k3/sku-blue-white.jpg', 1, 963),
    (30010, 20005, 'KeyForge K3 茶轴 黑色', '{"轴体":"茶轴","颜色":"黑色","连接":"三模"}', 469.00, 539.00, 'https://cdn.personaflow.local/catalog/products/keyforge-k3/sku-brown-black.jpg', 1, 887),
    (30011, 20006, 'SilentPro M8 白色', '{"颜色":"白色","连接":"双模"}', 129.00, 169.00, 'https://cdn.personaflow.local/catalog/products/silentpro-m8/sku-white.jpg', 1, 1742),
    (30012, 20006, 'SilentPro M8 深灰', '{"颜色":"深灰","连接":"双模"}', 129.00, 169.00, 'https://cdn.personaflow.local/catalog/products/silentpro-m8/sku-gray.jpg', 1, 1655),
    (30013, 20007, 'ViewMate 27 4K 标准支架', '{"尺寸":"27 英寸","支架":"标准支架"}', 1699.00, 1999.00, 'https://cdn.personaflow.local/catalog/products/viewmate-27-4k/sku-standard.jpg', 1, 371),
    (30014, 20007, 'ViewMate 27 4K 升降支架', '{"尺寸":"27 英寸","支架":"升降旋转支架"}', 1899.00, 2199.00, 'https://cdn.personaflow.local/catalog/products/viewmate-27-4k/sku-ergonomic.jpg', 1, 426),
    (30015, 20008, 'ErgoLift 支架 银色', '{"颜色":"银色","适配":"11-17 英寸"}', 99.00, 129.00, 'https://cdn.personaflow.local/catalog/products/ergolift-stand/sku-silver.jpg', 1, 2210),
    (30016, 20008, 'ErgoLift 支架 深灰', '{"颜色":"深灰","适配":"11-17 英寸"}', 109.00, 139.00, 'https://cdn.personaflow.local/catalog/products/ergolift-stand/sku-gray.jpg', 1, 1906),
    (30017, 20009, 'HomeEase E7 标准款 黑色', '{"颜色":"黑色","版本":"标准款"}', 1299.00, 1599.00, 'https://cdn.personaflow.local/catalog/products/homeease-e7/sku-black-standard.jpg', 1, 318),
    (30018, 20009, 'HomeEase E7 升级款 灰色', '{"颜色":"灰色","版本":"升级款","扶手":"4D 可调"}', 1599.00, 1899.00, 'https://cdn.personaflow.local/catalog/products/homeease-e7/sku-gray-pro.jpg', 1, 275),
    (30019, 20010, 'LinenNest 四件套 奶油白 1.5m', '{"颜色":"奶油白","床型":"1.5m"}', 329.00, 399.00, 'https://cdn.personaflow.local/catalog/products/linennest-cotton-set/sku-white-150.jpg', 1, 744),
    (30020, 20010, 'LinenNest 四件套 雾蓝 1.8m', '{"颜色":"雾蓝","床型":"1.8m"}', 369.00, 459.00, 'https://cdn.personaflow.local/catalog/products/linennest-cotton-set/sku-blue-180.jpg', 1, 692),
    (30021, 20011, 'PureSteam S1 白色', '{"颜色":"白色","水箱":"180ml"}', 189.00, 239.00, 'https://cdn.personaflow.local/catalog/products/puresteam-s1/sku-white.jpg', 1, 835),
    (30022, 20011, 'PureSteam S1 薄荷绿', '{"颜色":"薄荷绿","水箱":"180ml"}', 199.00, 249.00, 'https://cdn.personaflow.local/catalog/products/puresteam-s1/sku-green.jpg', 1, 602),
    (30023, 20012, 'FreshLock 5 件套 透明', '{"件数":"5 件套","颜色":"透明"}', 149.00, 189.00, 'https://cdn.personaflow.local/catalog/products/freshlock-glass-box/sku-5pcs.jpg', 1, 1189),
    (30024, 20012, 'FreshLock 8 件套 透明', '{"件数":"8 件套","颜色":"透明"}', 219.00, 279.00, 'https://cdn.personaflow.local/catalog/products/freshlock-glass-box/sku-8pcs.jpg', 1, 861),
    (30025, 20013, 'RunFlex Air 男款 黑白 42', '{"款式":"男款","颜色":"黑白","尺码":"42"}', 399.00, 499.00, 'https://cdn.personaflow.local/catalog/products/runflex-air/sku-men-42.jpg', 1, 540),
    (30026, 20013, 'RunFlex Air 女款 米白 38', '{"款式":"女款","颜色":"米白","尺码":"38"}', 399.00, 499.00, 'https://cdn.personaflow.local/catalog/products/runflex-air/sku-women-38.jpg', 1, 512),
    (30027, 20014, 'CoreFit 瑜伽垫 6mm 雾紫', '{"厚度":"6mm","颜色":"雾紫"}', 89.00, 119.00, 'https://cdn.personaflow.local/catalog/products/corefit-yoga-mat/sku-purple-6mm.jpg', 1, 1435),
    (30028, 20014, 'CoreFit 瑜伽垫 8mm 海盐蓝', '{"厚度":"8mm","颜色":"海盐蓝"}', 109.00, 139.00, 'https://cdn.personaflow.local/catalog/products/corefit-yoga-mat/sku-blue-8mm.jpg', 1, 1276),
    (30029, 20015, 'TrailPeak 冲锋衣 黑色 M', '{"颜色":"黑色","尺码":"M"}', 499.00, 699.00, 'https://cdn.personaflow.local/catalog/products/trailpeak-jacket/sku-black-m.jpg', 1, 358),
    (30030, 20015, 'TrailPeak 冲锋衣 沙岩色 L', '{"颜色":"沙岩色","尺码":"L"}', 529.00, 729.00, 'https://cdn.personaflow.local/catalog/products/trailpeak-jacket/sku-sand-l.jpg', 1, 322),
    (30031, 20016, 'HydroTrail 水壶 600ml 石墨黑', '{"容量":"600ml","颜色":"石墨黑"}', 119.00, 159.00, 'https://cdn.personaflow.local/catalog/products/hydrotrail-bottle/sku-black-600.jpg', 1, 980),
    (30032, 20016, 'HydroTrail 水壶 900ml 冰川蓝', '{"容量":"900ml","颜色":"冰川蓝"}', 149.00, 189.00, 'https://cdn.personaflow.local/catalog/products/hydrotrail-bottle/sku-blue-900.jpg', 1, 764),
    (30033, 20017, 'Mountain Roast 咖啡豆 250g', '{"规格":"250g","研磨":"原豆"}', 68.00, 88.00, 'https://cdn.personaflow.local/catalog/products/mountain-roast-beans/sku-250g.jpg', 1, 2034),
    (30034, 20017, 'Mountain Roast 咖啡豆 500g', '{"规格":"500g","研磨":"原豆"}', 128.00, 158.00, 'https://cdn.personaflow.local/catalog/products/mountain-roast-beans/sku-500g.jpg', 1, 1619),
    (30035, 20018, 'CalmLeaf 乌龙茶 20 袋', '{"规格":"20 袋","包装":"独立袋泡"}', 59.00, 79.00, 'https://cdn.personaflow.local/catalog/products/calmleaf-oolong/sku-20bags.jpg', 1, 1288),
    (30036, 20018, 'CalmLeaf 乌龙茶 40 袋', '{"规格":"40 袋","包装":"独立袋泡"}', 99.00, 129.00, 'https://cdn.personaflow.local/catalog/products/calmleaf-oolong/sku-40bags.jpg', 1, 941),
    (30037, 20019, 'NutriMix 每日坚果 30 袋', '{"规格":"30 袋","口味":"原味轻烘"}', 109.00, 139.00, 'https://cdn.personaflow.local/catalog/products/nutrimix-nuts/sku-30bags.jpg', 1, 2215),
    (30038, 20019, 'NutriMix 每日坚果 60 袋', '{"规格":"60 袋","口味":"原味轻烘"}', 199.00, 249.00, 'https://cdn.personaflow.local/catalog/products/nutrimix-nuts/sku-60bags.jpg', 1, 1762),
    (30039, 20020, 'BerryOat 能量棒 12 条', '{"规格":"12 条","口味":"莓果燕麦"}', 49.00, 69.00, 'https://cdn.personaflow.local/catalog/products/berryoat-bar/sku-12bars.jpg', 1, 1890),
    (30040, 20020, 'BerryOat 能量棒 24 条', '{"规格":"24 条","口味":"莓果燕麦"}', 89.00, 119.00, 'https://cdn.personaflow.local/catalog/products/berryoat-bar/sku-24bars.jpg', 1, 1471);

INSERT INTO product_category (id, name, parent_id, `level`, sort_order, status, icon_url)
VALUES
    (203, '办公外设', 2, 2, 23, 1, '/product-images/keyboard.svg'),
    (303, '睡眠家居', 3, 2, 33, 1, '/product-images/pillow.svg'),
    (403, '旅行收纳', 4, 2, 43, 1, '/product-images/backpack.svg'),
    (404, '运动恢复', 4, 2, 44, 1, '/product-images/yoga-mat.svg'),
    (503, '咖啡生活', 5, 2, 53, 1, '/product-images/coffee-machine.svg')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    parent_id = VALUES(parent_id),
    `level` = VALUES(`level`),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    icon_url = VALUES(icon_url);

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
    (21001, 303, '眠丘记忆棉枕头', '慢回弹承托颈椎，适合睡眠升级场景', 'MianQiu', '高低曲线分区承托，适合久坐办公人群改善睡眠舒适度。', '/product-images/pillow.svg', '["/product-images/pillow.svg"]', '{"材质":"慢回弹记忆棉","适用场景":"睡眠,颈椎承托","配套":"枕套,四件套,香薰灯"}', '睡眠,枕头,记忆棉,配套枕套', 1, 21001),
    (21002, 303, '眠丘亲肤枕套', '凉感棉柔面料，适配记忆棉枕头', 'MianQiu', '可拆洗枕套，适合作为枕头购买后的替换和清洁配套。', '/product-images/pillowcase.svg', '["/product-images/pillowcase.svg"]', '{"材质":"棉柔纤维","适用":"记忆棉枕头","配套":"枕头,四件套"}', '枕套,睡眠,清洁替换', 1, 21002),
    (21003, 303, 'LinenNest 柔棉四件套', '亲肤床品，适合睡眠家居整体升级', 'LinenNest', '柔棉床单、被套和枕套组合，适合与枕头和助眠灯搭配。', '/product-images/bedding.svg', '["/product-images/bedding.svg"]', '{"面料":"柔棉","规格":"1.5m/1.8m","配套":"枕头,香薰灯"}', '四件套,床品,睡眠,家居', 1, 21003),
    (21004, 303, 'DreamGlow 香薰助眠灯', '暖光香薰，营造睡前放松环境', 'DreamGlow', '柔和暖光、定时关闭和香薰扩散，适合作为睡眠用品的互补配套。', '/product-images/aroma-lamp.svg', '["/product-images/aroma-lamp.svg"]', '{"灯光":"暖光","功能":"香薰,定时","适用场景":"卧室,睡前放松"}', '香薰灯,助眠,睡眠,卧室', 1, 21004),

    (21005, 203, 'KeyForge K7 机械键盘', '三模连接，适合办公和桌面效率升级', 'KeyForge', '紧凑配列、热插拔轴体和低延迟连接，适合搭配鼠标、腕托和桌垫。', '/product-images/keyboard.svg', '["/product-images/keyboard.svg"]', '{"连接":"蓝牙/2.4G/有线","布局":"87键","配套":"鼠标,腕托,桌垫"}', '键盘,办公外设,桌面,效率', 1, 21005),
    (21006, 203, 'SilentPro 无线鼠标', '静音微动，适合办公室和图书馆', 'SilentPro', '轻量人体工学鼠标，适合与机械键盘和桌垫搭配。', '/product-images/mouse.svg', '["/product-images/mouse.svg"]', '{"连接":"2.4G/蓝牙","按键":"静音","配套":"键盘,桌垫,腕托"}', '鼠标,办公外设,静音,桌面', 1, 21006),
    (21007, 203, 'DeskFlow 大号桌垫', '防滑耐磨，统一桌面操作区域', 'DeskFlow', '大尺寸桌垫适合键盘、鼠标和显示器支架组合桌面。', '/product-images/deskmat.svg', '["/product-images/deskmat.svg"]', '{"尺寸":"900x400mm","材质":"细纹布面","配套":"键盘,鼠标,腕托"}', '桌垫,办公外设,桌面收纳', 1, 21007),
    (21008, 203, 'ErgoLift 显示器支架', '抬高视线，释放桌面空间', 'ErgoLift', '多层收纳显示器支架，适合与键盘、鼠标和桌垫组成办公套装。', '/product-images/monitor-stand.svg', '["/product-images/monitor-stand.svg"]', '{"材质":"铝合金","功能":"抬高,收纳","配套":"键盘,鼠标,桌垫"}', '显示器支架,办公外设,人体工学', 1, 21008),
    (21009, 203, 'SoftRest 记忆棉腕托', '缓解长时间打字手腕压力', 'SoftRest', '低回弹腕托适合机械键盘用户，减少桌面工作疲劳。', '/product-images/wrist-rest.svg', '["/product-images/wrist-rest.svg"]', '{"材质":"记忆棉","适用":"机械键盘","配套":"键盘,鼠标,桌垫"}', '腕托,键盘配件,办公外设', 1, 21009),

    (21010, 403, 'UrbanCarry 通勤背包', '电脑仓与通勤分区，适合短途旅行', 'UrbanCarry', '轻量防泼水背包，适合搭配数码收纳包、移动电源和分装瓶。', '/product-images/backpack.svg', '["/product-images/backpack.svg"]', '{"容量":"22L","功能":"电脑仓,防泼水","配套":"数码收纳包,移动电源,分装瓶"}', '背包,旅行,通勤,收纳', 1, 21010),
    (21011, 403, 'PackMate 数码收纳包', '数据线、充电器、耳机集中收纳', 'PackMate', '多隔层数码收纳包，适合背包和旅行场景。', '/product-images/organizer.svg', '["/product-images/organizer.svg"]', '{"隔层":"多隔层","适用":"线材,充电器,耳机","配套":"背包,移动电源"}', '数码收纳包,旅行收纳,配件', 1, 21011),
    (21012, 403, 'Voltix 轻薄移动电源', '10000mAh，通勤和旅行备用电量', 'Voltix', '轻薄移动电源，适合搭配背包和数码收纳包出行。', '/product-images/power-bank.svg', '["/product-images/power-bank.svg"]', '{"容量":"10000mAh","接口":"USB-C","配套":"背包,数码收纳包"}', '移动电源,旅行,通勤,充电', 1, 21012),
    (21013, 403, 'TravelLite 旅行分装瓶', '洗护分装，短途出行更轻便', 'TravelLite', '硅胶分装瓶套装，适合旅行背包和收纳包搭配。', '/product-images/travel-bottles.svg', '["/product-images/travel-bottles.svg"]', '{"件数":"4件套","材质":"硅胶","配套":"背包,收纳包"}', '分装瓶,旅行收纳,洗护', 1, 21013),

    (21014, 503, 'BrewDaily 胶囊咖啡机', '一键萃取，适合家庭和办公室咖啡角', 'BrewDaily', '小体积胶囊咖啡机，适合搭配咖啡豆、滤纸和保温杯形成咖啡生活场景。', '/product-images/coffee-machine.svg', '["/product-images/coffee-machine.svg"]', '{"类型":"胶囊咖啡机","场景":"家庭,办公室","配套":"咖啡豆,滤纸,保温杯"}', '咖啡机,咖啡生活,办公室,家用', 1, 21014),
    (21015, 503, 'Mountain Roast 咖啡豆', '中度烘焙，坚果与焦糖风味', 'Mountain Roast', '适合手冲、美式和意式拼配，是咖啡机后的持续补充需求。', '/product-images/coffee-beans.svg', '["/product-images/coffee-beans.svg"]', '{"烘焙":"中度","风味":"坚果,焦糖","配套":"咖啡机,滤纸,保温杯"}', '咖啡豆,咖啡生活,复购,补充', 1, 21015),
    (21016, 503, 'BrewDaily 原木浆滤纸', '稳定萃取，适配日常手冲', 'BrewDaily', '原木浆咖啡滤纸，适合咖啡豆和咖啡机用户的耗材补充。', '/product-images/coffee-filter.svg', '["/product-images/coffee-filter.svg"]', '{"规格":"100枚","适用":"手冲咖啡","配套":"咖啡豆,保温杯"}', '滤纸,咖啡耗材,咖啡生活', 1, 21016),
    (21017, 503, 'ThermoGo 随行保温杯', '锁温防漏，适合通勤咖啡', 'ThermoGo', '轻量保温杯，适合咖啡机用户把咖啡带到通勤和办公室。', '/product-images/thermos.svg', '["/product-images/thermos.svg"]', '{"容量":"480ml","保温":"6小时","配套":"咖啡机,咖啡豆"}', '保温杯,咖啡生活,通勤', 1, 21017),

    (21018, 404, 'CoreFit 防滑瑜伽垫', '稳定支撑，适合居家训练和拉伸', 'CoreFit', '环保 TPE 瑜伽垫，适合搭配运动水杯、筋膜球和速干毛巾。', '/product-images/yoga-mat.svg', '["/product-images/yoga-mat.svg"]', '{"材质":"TPE","厚度":"8mm","配套":"运动水杯,筋膜球,速干毛巾"}', '瑜伽垫,运动恢复,居家训练', 1, 21018),
    (21019, 404, 'RecoverPro 筋膜球', '局部放松，适合运动后恢复', 'RecoverPro', '筋膜球适合瑜伽、跑步和力量训练后的局部放松。', '/product-images/massage-ball.svg', '["/product-images/massage-ball.svg"]', '{"材质":"高密度硅胶","适用":"肩颈,足底,背部","配套":"瑜伽垫,速干毛巾"}', '筋膜球,运动恢复,放松', 1, 21019),
    (21020, 404, 'HydroTrail 运动水杯', '大容量防漏，训练时补水更方便', 'HydroTrail', '运动水杯适合健身、瑜伽和户外训练搭配。', '/product-images/sports-bottle.svg', '["/product-images/sports-bottle.svg"]', '{"容量":"750ml","功能":"防漏,刻度","配套":"瑜伽垫,速干毛巾"}', '运动水杯,运动恢复,补水', 1, 21020),
    (21021, 404, 'DryFast 速干毛巾', '轻量快干，适合训练和旅行', 'DryFast', '柔软速干毛巾，适合瑜伽、健身和旅行收纳。', '/product-images/towel.svg', '["/product-images/towel.svg"]', '{"材质":"超细纤维","特点":"快干,轻量","配套":"瑜伽垫,运动水杯"}', '速干毛巾,运动恢复,旅行', 1, 21021)
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    name = VALUES(name),
    subtitle = VALUES(subtitle),
    brand = VALUES(brand),
    description = VALUES(description),
    main_image_url = VALUES(main_image_url),
    detail_images_json = VALUES(detail_images_json),
    attributes_json = VALUES(attributes_json),
    tags = VALUES(tags),
    status = VALUES(status),
    sort_order = VALUES(sort_order);

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
    (31001, 21001, '眠丘记忆棉枕头 标准款', '{"规格":"标准款","颜色":"云白"}', 169.00, 219.00, '/product-images/pillow.svg', 1, 536),
    (31002, 21002, '眠丘亲肤枕套 两只装', '{"规格":"两只装","颜色":"雾灰"}', 59.00, 79.00, '/product-images/pillowcase.svg', 1, 482),
    (31003, 21003, 'LinenNest 柔棉四件套 1.8m', '{"床型":"1.8m","颜色":"燕麦米"}', 399.00, 499.00, '/product-images/bedding.svg', 1, 438),
    (31004, 21004, 'DreamGlow 香薰助眠灯 暖白', '{"灯光":"暖白","容量":"180ml"}', 129.00, 169.00, '/product-images/aroma-lamp.svg', 1, 612),
    (31005, 21005, 'KeyForge K7 茶轴 深空灰', '{"轴体":"茶轴","连接":"三模"}', 489.00, 569.00, '/product-images/keyboard.svg', 1, 920),
    (31006, 21006, 'SilentPro 无线鼠标 米白', '{"颜色":"米白","连接":"双模"}', 139.00, 179.00, '/product-images/mouse.svg', 1, 1180),
    (31007, 21007, 'DeskFlow 大号桌垫 暗绿', '{"尺寸":"900x400mm","颜色":"暗绿"}', 89.00, 119.00, '/product-images/deskmat.svg', 1, 1044),
    (31008, 21008, 'ErgoLift 显示器支架 银色', '{"颜色":"银色","层数":"双层"}', 159.00, 199.00, '/product-images/monitor-stand.svg', 1, 780),
    (31009, 21009, 'SoftRest 记忆棉腕托 黑色', '{"颜色":"黑色","长度":"87键"}', 69.00, 89.00, '/product-images/wrist-rest.svg', 1, 863),
    (31010, 21010, 'UrbanCarry 通勤背包 22L', '{"容量":"22L","颜色":"石墨黑"}', 299.00, 399.00, '/product-images/backpack.svg', 1, 704),
    (31011, 21011, 'PackMate 数码收纳包 中号', '{"规格":"中号","颜色":"雾蓝"}', 79.00, 99.00, '/product-images/organizer.svg', 1, 932),
    (31012, 21012, 'Voltix 轻薄移动电源 10000mAh', '{"容量":"10000mAh","颜色":"白色"}', 149.00, 199.00, '/product-images/power-bank.svg', 1, 1106),
    (31013, 21013, 'TravelLite 旅行分装瓶 四件套', '{"件数":"4件套","颜色":"透明"}', 49.00, 69.00, '/product-images/travel-bottles.svg', 1, 759),
    (31014, 21014, 'BrewDaily 胶囊咖啡机 奶油白', '{"颜色":"奶油白","水箱":"700ml"}', 599.00, 699.00, '/product-images/coffee-machine.svg', 1, 388),
    (31015, 21015, 'Mountain Roast 咖啡豆 500g', '{"规格":"500g","烘焙":"中度"}', 128.00, 158.00, '/product-images/coffee-beans.svg', 1, 1460),
    (31016, 21016, 'BrewDaily 原木浆滤纸 100枚', '{"规格":"100枚","型号":"V60"}', 29.00, 39.00, '/product-images/coffee-filter.svg', 1, 1720),
    (31017, 21017, 'ThermoGo 随行保温杯 480ml', '{"容量":"480ml","颜色":"海盐蓝"}', 119.00, 159.00, '/product-images/thermos.svg', 1, 922),
    (31018, 21018, 'CoreFit 防滑瑜伽垫 8mm', '{"厚度":"8mm","颜色":"鼠尾草绿"}', 119.00, 159.00, '/product-images/yoga-mat.svg', 1, 1334),
    (31019, 21019, 'RecoverPro 筋膜球 双只装', '{"规格":"双只装","硬度":"中等"}', 39.00, 59.00, '/product-images/massage-ball.svg', 1, 990),
    (31020, 21020, 'HydroTrail 运动水杯 750ml', '{"容量":"750ml","颜色":"冰川蓝"}', 79.00, 109.00, '/product-images/sports-bottle.svg', 1, 1208),
    (31021, 21021, 'DryFast 速干毛巾 中号', '{"规格":"中号","颜色":"浅灰"}', 49.00, 69.00, '/product-images/towel.svg', 1, 1016)
ON DUPLICATE KEY UPDATE
    spu_id = VALUES(spu_id),
    sku_name = VALUES(sku_name),
    specs_json = VALUES(specs_json),
    price = VALUES(price),
    original_price = VALUES(original_price),
    image_url = VALUES(image_url),
    status = VALUES(status),
    sales_count = VALUES(sales_count);

INSERT INTO inventory_stock (sku_id, available_quantity, locked_quantity, sold_quantity)
SELECT id, 120, 0, 0
FROM product_sku
WHERE id BETWEEN 31001 AND 31021
ON DUPLICATE KEY UPDATE
    available_quantity = available_quantity;

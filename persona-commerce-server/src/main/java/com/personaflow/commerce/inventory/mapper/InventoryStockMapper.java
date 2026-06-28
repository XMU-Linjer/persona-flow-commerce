package com.personaflow.commerce.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaflow.commerce.inventory.entity.InventoryStockEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryStockMapper extends BaseMapper<InventoryStockEntity> {

    @Select("""
            SELECT id,
                   sku_id,
                   available_quantity,
                   locked_quantity,
                   sold_quantity,
                   created_at,
                   updated_at
            FROM inventory_stock
            WHERE sku_id = #{skuId}
            """)
    InventoryStockEntity selectBySkuId(@Param("skuId") Long skuId);

    @Update("""
            UPDATE inventory_stock
            SET available_quantity = available_quantity - #{quantity},
                locked_quantity = locked_quantity + #{quantity},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND available_quantity >= #{quantity}
            """)
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE inventory_stock
            SET locked_quantity = locked_quantity - #{quantity},
                available_quantity = available_quantity + #{quantity},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND locked_quantity >= #{quantity}
            """)
    int releaseLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE inventory_stock
            SET locked_quantity = locked_quantity - #{quantity},
                sold_quantity = sold_quantity + #{quantity},
                updated_at = NOW()
            WHERE sku_id = #{skuId}
              AND locked_quantity >= #{quantity}
            """)
    int confirmLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}

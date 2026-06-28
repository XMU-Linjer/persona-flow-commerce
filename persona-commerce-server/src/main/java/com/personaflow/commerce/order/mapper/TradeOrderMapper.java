package com.personaflow.commerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaflow.commerce.order.entity.TradeOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrderEntity> {

    @Update("""
            UPDATE trade_order
            SET status = #{canceledStatus},
                canceled_at = #{canceledAt},
                updated_at = #{canceledAt}
            WHERE id = #{orderId}
              AND user_id = #{userId}
              AND status = #{pendingStatus}
            """)
    int cancelPendingOrder(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId,
            @Param("pendingStatus") Integer pendingStatus,
            @Param("canceledStatus") Integer canceledStatus,
            @Param("canceledAt") LocalDateTime canceledAt
    );

    @Update("""
            UPDATE trade_order
            SET status = #{paidStatus},
                paid_at = #{paidAt},
                updated_at = #{paidAt}
            WHERE id = #{orderId}
              AND user_id = #{userId}
              AND status = #{pendingStatus}
            """)
    int markOrderPaid(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId,
            @Param("pendingStatus") Integer pendingStatus,
            @Param("paidStatus") Integer paidStatus,
            @Param("paidAt") LocalDateTime paidAt
    );
}

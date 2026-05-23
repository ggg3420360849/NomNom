package com.nomnom.backend.mapper;

import com.nomnom.backend.model.PackageSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PackageReadMapper {

    @Select("""
            <script>
            SELECT
                dp.id AS packageId,
                d.id AS dishesId,
                d.name AS dishName,
                COALESCE(dp.name, '') AS packageName,
                COALESCE(dp.specs, '') AS specs,
                COALESCE(d.cover_url, '') AS coverUrl,
                COALESCE(dp.price, 0) AS price,
                COALESCE(d.status, 0) AS dishStatus,
                COALESCE(d.stock, 999) AS stock
            FROM dishes_package dp
            JOIN dishes d ON d.id = dp.dishes_id
            WHERE dp.id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<PackageSnapshot> selectPackageSnapshots(@Param("ids") List<Long> ids);
}

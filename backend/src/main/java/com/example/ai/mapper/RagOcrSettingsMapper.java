package com.example.ai.mapper;

import com.example.ai.pojo.RagOcrUserSettings;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RagOcrSettingsMapper {

    @Select("""
            SELECT id,
                   user_id AS userId,
                   base_url AS baseUrl,
                   api_key AS apiKey,
                   model,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM rag_ocr_user_settings
            WHERE user_id = #{userId}
            """)
    RagOcrUserSettings selectByUserId(@Param("userId") Integer userId);

    @Insert("""
            INSERT INTO rag_ocr_user_settings (user_id, base_url, api_key, model)
            VALUES (#{userId}, #{baseUrl}, #{apiKey}, #{model})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RagOcrUserSettings settings);

    @Update("""
            UPDATE rag_ocr_user_settings
            SET base_url = #{baseUrl},
                api_key = #{apiKey},
                model = #{model},
                updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    int updateByUserId(RagOcrUserSettings settings);
}

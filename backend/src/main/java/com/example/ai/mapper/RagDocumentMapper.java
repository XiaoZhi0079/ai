package com.example.ai.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface RagDocumentMapper {

    @Insert("INSERT INTO rag_documents (file_name, oss_url, uploaded_by) VALUES (#{fileName}, #{ossUrl}, #{uploadedBy})")
    int insert(@Param("fileName") String fileName, @Param("ossUrl") String ossUrl, @Param("uploadedBy") Integer uploadedBy);

    @Select("SELECT id, file_name AS fileName, oss_url AS ossUrl, uploaded_by AS uploadedBy, created_at AS createdAt FROM rag_documents ORDER BY id DESC")
    List<Map<String, Object>> selectAll();

    @Delete("DELETE FROM rag_documents WHERE id = #{id}")
    int deleteById(Integer id);
}
